'use client';
import { Shell } from './shell';
import { HomePage } from './home';
import { SearchPage } from './search';
import { ContentEvaluationPage, PersonEvaluationPage, GenrePreferencesPage } from './evaluation';
import { QuickRatingPage } from './quick';
import { ModelPage } from './model';
import { ForYouPage, JointRecommendationPage } from './recommendations';
import { RatingsPage, SettingsPage } from './settings';
import { Empty } from './ui';
export function Routes({ route }: { route: string[] }) {
  let page;
  switch (route[0] || '') {
    case '': page = <HomePage />; break;
    case 'search': page = <SearchPage />; break;
    case 'content': page = <ContentEvaluationPage id={Number(route[1])} />; break;
    case 'people': page = <PersonEvaluationPage id={Number(route[1])} />; break;
    case 'genres': page = <GenrePreferencesPage />; break;
    case 'quick': page = <QuickRatingPage />; break;
    case 'model': page = <ModelPage />; break;
    case 'for-you': page = <ForYouPage />; break;
    case 'together': page = <JointRecommendationPage />; break;
    case 'ratings': page = <RatingsPage />; break;
    case 'settings': page = <SettingsPage />; break;
    default: page = <Empty title="Página não encontrada" message="Use o menu para continuar descobrindo." />;
  }
  return <Shell><div key={route.join('/')}>{page}</div></Shell>;
}
