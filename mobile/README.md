# Lume para Android e iOS

Flutter 3.47.2 / Dart 3.13.2, Material 3, Riverpod, Dio, go_router, SharedPreferences e cache de imagens. Todos os cálculos de recomendação e a normalização de avaliações ficam no backend.

Na raiz do repositório, os scripts reconhecem o SDK instalado em `.tools/flutter` e o Android SDK em `.tools/android-sdk`. Com Flutter disponível no PATH:

```powershell
cd mobile
flutter pub get
flutter analyze
flutter test
flutter build apk --release --dart-define=API_BASE_URL=http://IP_DO_PC:8080
```

APK: `build/app/outputs/flutter-apk/app-release.apk`. Para gerar usando o IPv4 ativo e verificar todas as etapas:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-android.ps1 -ApiBaseUrl http://IP_DO_PC:8080
```

O HTTP do APK é limitado ao host privado especificado no build. A política base bloqueia cleartext para os demais hosts. Um build com `API_BASE_URL=https://api.seu-dominio` não contém exceção HTTP. Para mudar o IP HTTP, gere outro APK; a tela de conexão também permite configurar endereços HTTPS.

Sem `android/key.properties`, o release é assinado com o certificado de desenvolvimento: instalável para testes, não preparado para publicação oficial na Play Store. Para assinatura oficial, crie um keystore seu e configure o arquivo ignorado pelo Git:

```properties
storeFile=C:/caminho/seguro/lume-upload.jks
storePassword=SUA_SENHA
keyAlias=lume
keyPassword=SUA_SENHA
```

```powershell
flutter build appbundle --release --dart-define=API_BASE_URL=https://api.seu-dominio
```

AAB: `build/app/outputs/bundle/release/app-release.aab`. Não envie um bundle com certificado de desenvolvimento à loja.

Para aparelho conectado por USB, habilite depuração USB e autorize o computador:

```powershell
flutter devices
flutter run --dart-define=API_BASE_URL=http://IP_DO_PC:8080
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

Para instalar manualmente, copie o APK para o Android, abra o arquivo, permita a instalação da fonte utilizada quando solicitado, instale e escolha o perfil Aliny. O computador e o aparelho precisam estar na mesma rede; backend e porta 8080 devem estar acessíveis.

O diretório `ios/` contém o projeto Xcode e o código compartilhado está preparado para iOS. **Não há build iOS no Windows.** Em macOS, use Flutter, Xcode e assinatura Apple apropriada, configure o bundle ID/Signing no Xcode e execute `flutter build ipa --release --dart-define=API_BASE_URL=https://api.seu-dominio`. O projeto inclui a descrição de acesso à rede local; builds de produção devem usar HTTPS.
