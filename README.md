# HAI913I — TP1 Analyse statique

Cet analyseur lit un projet Java, construit les faits de structure et d'appels avec
Eclipse JDT, puis calcule les métriques et le graphe d'appels demandés dans le TP1.
Les marqueurs `// A0` à `// B4` dans le code permettent de relier chaque composant
aux étapes du sujet.

## Environnement

- JDK d'exécution : OpenJDK `21.0.12` ;
- version cible de compilation : Java `17` ;
- Maven `3.9.6` ;
- Eclipse JDT Core `3.46.0` ;
- JUnit Jupiter `5.14.4` pour les tests.

Le projet est indépendant de l'IDE. Les commandes ci-dessous sont celles à utiliser
depuis le dossier `starter`.

## Construction depuis une copie fraîche

```text
mvn clean test
mvn clean package
```

Le JAR exécutable est alors créé dans `target/hai913i-tp1-analyzer.jar`.

## Utilisation

La syntaxe complète est affichée par une erreur de ligne de commande :

```text
java -jar target/hai913i-tp1-analyzer.jar PROJET \
  --action all|structure|calls|metrics|graph \
  --threshold X [--ast] [--callers IDENTIFIANT] [--callees IDENTIFIANT]
```

Exemples :

```text
# Tout produire, avec X = 5
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action all --threshold 5

# Vérifier A1, A2 et A3 séparément
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action structure --threshold 5 --ast
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action calls --threshold 5

# B2 avec un autre seuil
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action metrics --threshold 4

# B3 et requêtes dans les deux directions
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action graph --threshold 5 \
  --callers 'library.model.Loanable#checkOut(library.model.Member)'
java -jar target/hai913i-tp1-analyzer.jar "../resources/validation" \
  --action graph --threshold 5 \
  --callees 'library.util.TextUtils#repeat(java.lang.String,int)'
```

`--ast` affiche l'AST de `Dvd.java` et `Category.java` lorsqu'ils sont présents ;
sur un autre projet, il affiche les AST de tous les fichiers. Les chemins contenant
des espaces sont pris en charge s'ils sont entourés de guillemets.

## Résultats attendus sur `resources/validation`

La partie A0 annonce 14 unités de compilation et 0 erreur. A2 retrouve 15 types,
59 méthodes et 21 attributs. A3 retrouve 94 appels : 46 internes, 48 externes et
0 non résolu.

B2 donne :

```text
15 types · 493 lignes · 59 méthodes · 4 paquetages
3,93 méthodes/classe · 4,64 lignes/méthode avec corps · 1,40 attribut/classe
```

Avec `X = 5`, les classes sélectionnées sont `Item`, `Member` et `LoanService`.
Avec `X = 4`, six classes sont sélectionnées. Les 5 paramètres maximum sont
ceux de `library.util.Statistics#report(...)`.

B3 donne 59 nœuds, 44 arcs distincts, 46 sites internes, 48 externes et 0 non
résolu. Les arcs sont affichés sous la forme `appelant -> cible [poids]` ; un arc
regroupe plusieurs sites d'appel identiques.

## Organisation du code

```text
src/main/java/hai913i/tp1/
├── Main.java                    entrée et orchestration (A0, B4)
├── parse/                       découverte des sources et AST JDT (A0)
├── debug/                       visiteur d'affichage AST (A1)
├── extract/                     structure, appels et faits (A2, A3, B1)
├── model/                       TypeInfo, MethodInfo, appels et faits (B1)
├── metrics/                     calcul des 13 métriques (B2)
├── graph/                       nœuds, arcs, poids et requêtes (B3)
├── cli/                         validation des options (B4)
└── report/                      sorties textuelles (B2, B3)
```

`MetricsCalculator` et `CallGraph` ne dépendent d'aucune classe JDT : ils utilisent
uniquement `ProjectFacts`. Les identifiants de méthode contiennent le nom qualifié
du déclarant et les types qualifiés des paramètres, ce qui distingue les surcharges
et conserve les classes imbriquées. Les appels non résolus restent dans les faits,
mais ne deviennent pas des arcs.

## Robustesse et limites

Un chemin inexistant, un dossier sans fichier Java, une option inconnue ou un seuil
mal formé produisent un message explicite et un code de sortie non nul. Les erreurs
de syntaxe JDT sont signalées mais n'empêchent pas l'analyse des autres fichiers.
Un binding absent est conservé comme appel `UNRESOLVED`.

Le graphe est une approximation statique : il ne simule pas les appels virtuels,
la réflexion, les proxys ou l'injection de dépendances. Les appels vers des
bibliothèques extérieures sont comptés mais ne sont pas des nœuds du graphe.
Les appels vers les constructeurs ne sont pas ajoutés aux arcs, conformément à
l'option facultative du sujet ; les constructeurs restent toutefois des nœuds et
des méthodes comptées.

## Dépannage

- `Missing system library` : utiliser un JDK complet et relancer `mvn clean package`.
- `Aucun fichier Java` : donner le dossier du projet, pas directement un fichier.
- `Le seuil X est obligatoire` : ajouter `--threshold 5` (ou une autre valeur entière).
- Sortie avec des erreurs de compilation : l'outil poursuit l'extraction ; vérifier
  ensuite les lignes signalées et les éventuels appels non résolus.

Pour l'archive finale, supprimer les artefacts `target/` et caches d'IDE ; le dépôt
doit contenir uniquement les sources, le `pom.xml`, le README et les livrables.
