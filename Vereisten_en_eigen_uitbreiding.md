# Eigen werk: eisen en uitbreiding

Korte toelichting bij het beroepsproduct ICSS-compiler. De formele beschrijving staat in `ASSIGNMENT.md`; hieronder wat er gebouwd is en welke taaluitbreiding daar bovenop zit.

## Voldoening aan de opdracht

De implementatie volgt de opdrachtstructuur: ANTLR-grammatica en listener voor alle parser-niveaus (PA01–PA04), gebruik van de eigen `IHANStack`-implementatie voor AST (PA00), semantische analyse met scopes en de gevraagde checker-regels (CH01–CH06), evaluator in één traversal (TR01/TR02) en CSS-generatie met twee spaties inspringing per niveau (GE01/GE02).

## Taaluitbreiding (paragraaf 4.6)

Twee uitbreidingen die de volledige pipeline volgen (lexer → parser → AST → checker → evaluator → gegenereerde CSS).

### 1. Min en max op lengtes

`min(...)` en `max(...)` met twee operanden van hetzelfde type (pixels, percentage of scalaire waarde). De evaluator vouwt dit uit naar één literal; de generator hoeft daar geen aparte syntax voor te hebben.

Voorbeeld:

```text
p {
  width: min(40px, 100px);
}
```

Hier wordt in de output de kleinste waarde gebruikt, dus effectief `40px` in de CSS.

### 2. Vast type per variabele binnen een scope

Een tweede assignment aan dezelfde naam in dezelfde scope mag het type niet wijzigen (bijvoorbeeld eerst pixels, daarna percentage). De checker geeft daar een semantische fout voor.

Voorbeeld dat wordt afgekeurd:

```text
X := 10px;
X := 5%;
```

## Tests bij de uitbreiding

Naast de bestaande `ParserTest`-levels staat een aparte testklasse `ExtensionFeatureTest` in de startcode. Daarin zitten een paar korte tests die de volledige `Pipeline` gebruiken: of `min`/`max` na transform de verwachte waarde in de CSS-string zet, of de checker mixed operanden en type-wissels afwijst, en een check op percentages.

Die tests heb ik toegevoegd om gericht te kunnen controleren of de nieuwe taalregels echt door parse, check en eval heen lopen; de level-tests dekken dat niet.
