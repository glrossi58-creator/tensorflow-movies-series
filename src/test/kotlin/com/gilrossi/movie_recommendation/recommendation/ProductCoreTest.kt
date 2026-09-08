package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.discovery.*
import com.gilrossi.movie_recommendation.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

class ProductCoreTest {
    @Test fun `local search always precedes and suppresses external request`() = runTest {
        val local=mockk<LocalDiscoveryRepository>();val tmdb=mockk<TmdbClient>()
        val result=DiscoveryItem(id=88,tmdbId=603,type=DiscoveryType.MOVIE,title="The Matrix",source="LOCAL")
        coEvery { local.search("Matrix",null) } returns listOf(result)
        assertEquals(88,DiscoveryService(local,tmdb).search(" Matrix ").results.single().id)
        coVerify(exactly=0) { tmdb.searchUniversal(any(),any()) }
    }
    @Test fun `external results cached while local always rechecked`() = runTest {
        val local=mockk<LocalDiscoveryRepository>();val tmdb=mockk<TmdbClient>()
        coEvery { local.search("Show",DiscoveryType.SERIES) } returns emptyList()
        coEvery { tmdb.searchUniversal("Show",DiscoveryType.SERIES) } returns TmdbDiscoveryResponse(listOf(TmdbDiscoveryItem(10,name="Show")))
        val service=DiscoveryService(local,tmdb)
        assertEquals("TMDB",service.search("Show",DiscoveryType.SERIES).results.single().source)
        service.search("Show",DiscoveryType.SERIES)
        coVerify(exactly=1) { tmdb.searchUniversal(any(),any()) }
        coVerify(exactly=2) { local.search(any(),any()) }
    }
    @Test fun `creator import requires real created by relation`() = runTest {
        val local=mockk<LocalDiscoveryRepository>();val tmdb=mockk<TmdbClient>()
        val person=TmdbPersonDetails(99,"Creator")
        coEvery { tmdb.getPersonDetails(99) } returns person
        coEvery { tmdb.getPersonCredits(99) } returns TmdbPersonCredits(crew=listOf(TmdbPersonCredit(2,"Creator","tv"),TmdbPersonCredit(3,"Writer","tv")))
        coEvery { tmdb.getSeriesDetails(2) } returns com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse(2,"Show",null,emptyList(),null,null,listOf(com.gilrossi.movie_recommendation.dto.TmdbCreator(99,"Creator")))
        coEvery { local.importPerson(person,setOf(RatingTargetType.CREATOR)) } returns DiscoveryItem(id=45,type=DiscoveryType.PERSON,title="Creator",source="LOCAL",roles=setOf(RatingTargetType.CREATOR))
        assertEquals(setOf(RatingTargetType.CREATOR),DiscoveryService(local,tmdb).importPerson(99).roles)
        coVerify(exactly=0) { tmdb.getSeriesDetails(3) }
    }
    @Test fun `validation labels cannot influence training features`() {
        val normalizer=RatingNormalizer();val builder=RecommendationDatasetBuilder(FeatureBuilder(normalizer),normalizer)
        val ratings=(1L..10).map { Rating(it,1,RatingTargetType.MOVIE,(it%5+1).toInt(),it,null,null,Instant.EPOCH,Instant.EPOCH) }
        val signals=ratings.map { ContentSignals(Content(it.contentId,"Content",null,ContentType.MOVIE,null,null,null),setOf(1),setOf(2),emptySet()) }
        val (training,validation)=builder.split(ratings,signals,emptyMap())
        val changed=ratings.map { if(it.contentId in validation.map { v -> v.contentId }) it.copy(value=if(it.value==1) 5 else 1) else it }
        val (second,_)=builder.split(changed,signals,emptyMap())
        training.zip(second).forEach { (a,b) -> assertContentEquals(a.features,b.features); assertEquals(a.label,b.label) }
        assertTrue((training+validation).all { it.features.size==8 && it.features.all { v -> v in 0f..1f } })
        assertTrue(training.none { it.contentId in validation.map { v -> v.contentId } })
    }
    @Test fun `embeddings deterministic finite dimension eight`() {
        val s=ContentSignals(Content(1,"Legacy",null,ContentType.SERIES,null,null,null),setOf(2,3),setOf(4,5),setOf(6))
        val generator=ContentEmbeddingGenerator()
        val a=generator.generate(s)
        assertEquals(8,a.size);assertTrue(a.all { it.isFinite() });assertContentEquals(a,generator.generate(s))
        assertEquals(1.0,a.sumOf { it.toDouble()*it },0.00001)
    }
}
