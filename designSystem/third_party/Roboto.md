# Roboto compartilhada

Fonte de referência: `Roboto-Regular.ttf` do layoutlib distribuído no Android
Studio (`plugins/design-tools/resources/layoutlib/data/fonts/`), versão
**3.015; 2026**, Roboto Project Authors. SHA-256 do original:
`d5633ff957d2ecb1ce7716e86e11057091c3526042efe16fe1f65dfc06a65551`.

Os pesos estáticos 400, 500, 600, 700 e 800 foram instanciados com FontTools
4.61.1, mantendo largura 100 e estilo normal. A forma das letras vem do mesmo
arquivo de referência Android; não há download de fontes durante o uso do app.
Tamanhos, pesos, altura de linha e espaçamento dos estilos Material são mantidos.

Reprodução opcional, com FontTools instalado no ambiente de ferramentas:

```sh
python scripts/prepare-shared-fonts.py --source /caminho/Roboto-Regular.ttf
```

O script valida hash, pesos, ausência de eixos variáveis e caracteres brasileiros.
O build normal usa os TTF já incluídos e não depende de Python ou FontTools.

[Projeto da fonte](https://github.com/googlefonts/roboto-3-classic).
Licença SIL OFL 1.1: [cópia incluída nos recursos](../src/commonMain/composeResources/files/roboto/OFL.txt).
