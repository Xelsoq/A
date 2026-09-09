# SimpMusic — Samsung Music UI (Now Playing) — merge ready

## Como aplicar

1. Descompacte este zip **na raiz do repositório SimpMusic** (ou copie os paths relativos).
2. O módulo `core` é submodule: o arquivo
   `core/domain/src/commonMain/kotlin/com/maxrave/domain/manager/DataStoreManager.kt`
   deve ir para o clone do **core** (mesmo path dentro de `core/`).
3. Rebuild / Sync Gradle (para regenerar `Res.string.now_playing_style_samsung`).
4. Abra o app → **Settings → Now Playing style → Samsung Music**.

## Arquivos neste zip

### Novos
- `composeApp/.../NowPlayingContentSamsung.kt`
- `composeApp/.../SamsungColors.kt`

### Modificados (já com patch)
- `composeApp/.../NowPlayingScreen.kt`
- `composeApp/.../SettingScreen.kt`
- `composeApp/.../composeResources/values/strings.xml`
- `core/domain/.../DataStoreManager.kt`

## Nada para apagar

## Observação
Playback continua YouTube Music (handler do SimpMusic). Só a UI do full player muda.
