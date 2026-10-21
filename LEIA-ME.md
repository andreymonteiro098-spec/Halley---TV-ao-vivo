# Halley Play TV — app para TV TCL (Google TV / Android TV)

Este projeto transforma o site https://play.halleytelecom.com.br em um aplicativo
que aparece na tela inicial da sua TV, abre em tela cheia e pode ser controlado
com o controle remoto.

## Parte 1 — Gerar o APK (de graça, pelo navegador, sem instalar nada)

1. Crie uma conta gratuita em https://github.com (se ainda não tiver).
2. Clique em **+ > New repository**. Dê o nome `halley-play-tv`, marque **Public**
   e clique em **Create repository**.
3. Na página do repositório, clique em **uploading an existing file**.
4. Descompacte o arquivo ZIP no computador e arraste **todo o conteúdo** da pasta
   `HalleyPlayTV` (inclusive a pasta `.github`) para a página. Clique em **Commit changes**.
   - Se a pasta `.github` não aparecer na lista (ela é oculta em alguns sistemas),
     crie o arquivo manualmente: **Add file > Create new file**, digite o nome
     `.github/workflows/build.yml` e cole o conteúdo do arquivo de mesmo nome.
5. Abra a aba **Actions**. O trabalho "Gerar APK" começa sozinho (leva de 3 a 6 minutos).
   Se não começar, clique em "Gerar APK" e depois em **Run workflow**.
6. Quando ficar com o ícone verde, abra **Releases** (coluna da direita na página
   inicial do repositório). Lá estará o arquivo **HalleyPlay-TV.apk**.
   O endereço direto de download será:

   `https://github.com/SEU-USUARIO/halley-play-tv/releases/latest/download/HalleyPlay-TV.apk`

   (troque SEU-USUARIO pelo seu nome de usuário do GitHub).

## Parte 2 — Instalar na TV TCL

Método mais fácil, com o app **Downloader**:

1. Na TV, abra a **Google Play Store**, procure por **Downloader** (ícone laranja,
   da AFTVnews) e instale.
2. Vá em **Configurações > Sistema > Segurança e restrições > Fontes desconhecidas**
   (o nome do menu muda um pouco entre modelos) e permita para o **Downloader**.
3. Abra o Downloader, digite o endereço de download do APK (Parte 1, passo 6) e
   confirme. Quando baixar, clique em **Instalar**.
4. Pronto: o app **Halley Play** aparece na lista de aplicativos da TV.

Alternativa com pendrive: copie o `HalleyPlay-TV.apk` para um pendrive, espete na TV,
instale um gerenciador de arquivos pela Play Store (por exemplo, "X-plore") e abra o APK.

## Como usar com o controle remoto

| Botão | O que faz |
|---|---|
| Setas | Movem o cursor (seta branca). Quando chega na borda, rola a página |
| OK | Clica onde o cursor está |
| Segurar OK por 1 segundo | Troca entre **modo cursor** e **modo navegação por foco** |
| Voltar | Volta uma página; sai do vídeo em tela cheia; na primeira página, aperte duas vezes para sair |
| Play/Pause | Pausa ou retoma o vídeo |

Para digitar login e senha, clique no campo com o cursor: o teclado da TV aparece.

## Ajustes comuns (para quem for editar o código)

- Endereço do site: `SITE_URL` em `app/src/main/java/.../MainActivity.java`
- Nome do app: `android:label` em `app/src/main/AndroidManifest.xml`
- Ícone e banner: arquivos `ic_launcher.png` (pastas `mipmap-*`) e `banner.png`
  (pasta `drawable-xhdpi`, 320x180). Troque pelo logo da Halley Telecom se quiser.

## Se algo não funcionar

- **Tela preta ou site quebrado:** atualize o "Android System WebView" na Play Store da TV.
- **Site aparece muito grande ou pequeno, ou em versão de celular:** avise para eu
  ajustar o zoom ou o identificador do navegador no app.
- **O APK não instala:** confirme que permitiu "Fontes desconhecidas" para o app
  que está fazendo a instalação (Downloader).
