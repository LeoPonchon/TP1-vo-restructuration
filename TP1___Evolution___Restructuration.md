\documentclass[12pt,a4paper]{report}

\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}
\usepackage[french]{babel}
\usepackage{geometry}
\geometry{margin=2cm}
\usepackage{hyperref}
\hypersetup{
  colorlinks=true,
  linkcolor=black,
  urlcolor=blue,
  pdftitle={Rapport TP1 -- Analyse statique},
  pdfauthor={Léo Ponchon et Yasser Razzaki}
}
\usepackage{setspace}
\usepackage{enumitem}
\usepackage{array}
\usepackage{booktabs}
\usepackage{graphicx}
\usepackage{xcolor}
\usepackage{listings}
\usepackage{float}
\usepackage[none]{hyphenat}
\usepackage{comment}
\usepackage{minted}

\onehalfspacing
\setcounter{secnumdepth}{0}
\newcommand{\code}[1]{\texttt{#1}}

\definecolor{codegray}{RGB}{245,245,245}
\definecolor{commentgray}{RGB}{90,90,90}
\lstdefinestyle{code}{
  backgroundcolor=\color{codegray},
  basicstyle=\ttfamily\small,
  breaklines=true,
  frame=single,
  columns=fullflexible,
  keepspaces=true,
  commentstyle=\color{commentgray}
}

\begin{document}

\begin{titlepage}
    \centering
    \vspace*{2cm}

    \includegraphics[width=0.75\textwidth]{logo.png}\par\vspace{1cm}

    {\Large Université de Montpellier\\
    Faculté des Sciences\par}
    \vspace{1.5cm}

    {\Huge\bfseries Rapport de TP1\par}
    \vspace{0.5cm}
    {\LARGE Analyse statique de projets Java\par}
    \vspace{1cm}
    {\large HAI913I -- Évolution et restructuration des logiciels\par}
    \vspace{0.5cm}
    {\large Enseignant responsable : Bachar Rima\par}

    \vfill

    \begin{tabular}{lll}
        \textbf{Membre} & \textbf{N° étudiant} & \textbf{Parcours} \\
        Léo Ponchon & 22009418 & GL \\
        Yasser Razzaki & à compléter & GL \\
    \end{tabular}

    \vspace{1cm}

    Année universitaire : 2026 - 2027\\
    Date : \today\\
    Dépôt Git : \href{https://github.com/LeoPonchon/TP1-vo-restructuration}{GitHub}

    \vspace{1cm}
\end{titlepage}

\tableofcontents
\clearpage

\section{Résumé du projet}

Nous avons développé un analyseur statique de projets Java. Le programme reçoit
un chemin de projet, construit son AST avec Eclipse JDT, extrait les informations
structurelles et les appels de méthodes, puis calcule les métriques demandées et
un graphe d'appels.

La réalisation couvre les étapes A0 à B4 :

\begin{itemize}
  \item configuration de l'environnement et parsing avec les bindings JDT.
  \item parcours et affichage de l'AST.
  \item extraction des types, attributs, méthodes et appels.
  \item construction d'un modèle indépendant de JDT.
  \item calcul des métriques et génération du graphe d'appels.
  \item interface en ligne de commande, rapports textuels et gestion des erreurs.
\end{itemize}

\section{PARTIE A - Construire l’extracteur de faits}

\subsection{A0 -- Environnement et projet de validation}

Le projet utilise Maven. L'environnement de développement est :

\begin{center}
\begin{tabular}{ll}
\toprule
Java utilisé pour l'exécution & OpenJDK 21.0.12 \\
Compilation cible & Java 17 \\
Maven & 3.9.6 \\
Dépendances principales & Eclipse JDT Core 3.46.0 \\
\bottomrule
\end{tabular}
\end{center}

La première étape consiste à préparer l'environnement de développement en vérifiant que les versions de Java et de Maven sont correctement installées. Une fois cette vérification effectuée, le projet de validation situé dans resources/validation est compilé puis exécuté via Maven \code{mvn -q compile exec:java} afin de prendre connaissance du code source qui servira de sujet d'étude tout au long du TP. 

Enfin, le projet d'analyseur de code (\code{starter}, configuré avec la dépendance \textit{Eclipse JDT Core 3.46.0}, est compilé avec la commande \code{mvn clean package}. 

L'exécutable généré \textit{hai913i-tp1-analyzer.jar }est ensuite appliqué au projet de validation afin d'en analyser l'arbre syntaxique abstrait \textit{(AST)} et de vérifier que l'analyseur identifie bien la racine des sources, les 14 unités de compilation et l'absence d'erreurs.

Sur le projet \code{resources/validation}, le résultat est de 14 unités de
compilation et 0 erreur.

\begin{figure}[H]
    \centering
    \includegraphics[width=1\linewidth]{a0.png}
    \caption{Construction / Build avec Maven}
    \label{fig:placeholder}
\end{figure}

\subsection{A1 -- Parcours de l'AST}
\subsubsection{1) La sous-classe d'ASTVisitor}
Afin d'inspecter la structure syntaxique des fichiers Java du projet de validation, nous avons implémenté la classe \texttt{PrintAst}, qui hérite de \texttt{ASTVisitor}. Pour éviter de redéfinir manuellement les méthodes de visite pour chaque type de nœud spécifique de l'AST Eclipse JDT, nous avons tiré parti des méthodes génériques \texttt{preVisit2} et \texttt{postVisit}. 

La méthode \texttt{preVisit2} récupère le nom simple de la classe du nœud via 

\texttt{node.getClass().getSimpleName()}, l'affiche avec une indentation proportionnelle à la variable \texttt{depth}, puis incrémente cette profondeur de 1. Une fois l'exploration des nœuds enfants terminée, la méthode \texttt{postVisit} décrémente \texttt{depth}, ce qui permet de remonter l'arbre syntaxique en conservant un alignement visuel structuré et lisible.

\subsubsection{2) Repérage des nœuds (\texttt{Dvd.java} et \texttt{Category.java})}

L'inspection des arbres syntaxiques générés par notre visiteur permet d'identifier clairement les différents constituants d'un fichier source Java :
\begin{itemize}
    \item \textbf{La classe / énumération :} Elle est représentée par un nœud de type \texttt{TypeDeclaration} pour une classe classique comme \texttt{Dvd.java}. En revanche, pour un type énuméré (\texttt{Category.java}), l'arbre présente un nœud racine spécifique de type \texttt{EnumDeclaration} (ce qui illustre que les énumérations ne sont pas capturées par une simple visite de type \texttt{TypeDeclaration}).
    \item \textbf{Les attributs :} Ils correspondent aux nœuds de type \texttt{FieldDeclaration}, souvent associés à des sous-nœuds de type \texttt{VariableDeclarationFragment} pour nommer le champ (par exemple, l'attribut \texttt{minutes} dans \texttt{Dvd}, ou l'attribut \texttt{label} dans \texttt{Category}).
    \item \textbf{Les méthodes :} Elles sont encapsulées dans des nœuds \texttt{MethodDeclaration}, qui englobent les constructeurs (contenant parfois des appels explicites comme 
    
    \texttt{SuperConstructorInvocation} dans \texttt{Dvd}) ainsi que les méthodes métiers ou accesseurs.
    \item \textbf{Les appels :} Les instructions effectuant un appel de méthode ou d'invocation apparaissent sous forme de nœuds spécifiques intégrés dans les blocs d'instructions.
\end{itemize}

\subsubsection{3) Rôle de la valeur de retour de \texttt{visit()} et de \texttt{return false}}

Dans le patron Visiteur d'Eclipse JDT, la valeur booléenne renvoyée par les méthodes de visite (\texttt{visit(...)}) joue un rôle fondamental dans la stratégie de parcours de l'arbre syntaxique :
\begin{itemize}
    \item \textbf{Retourner \texttt{true} :} Indique à l'analyseur de poursuivre le parcours récursif en descendant dans les sous-nœuds (les enfants) du nœud courant.
    \item \textbf{Retourner \texttt{false} :} Permet d'\emph{élaguer} l'arbre. L'analyseur ignore complètement les enfants du nœud courant et passe directement au frère suivant, ce qui s'avère particulièrement utile pour cibler des éléments précis sans parcourir inutilement toute leur structure interne.
\end{itemize}

\subsection{A2 — Extraction de la structure du projet}

Pour répondre aux exigences de la section A2, nous avons implémenté la classe

\texttt{StructureExtractor} afin d'extraire de manière exhaustive la structure orientée objet du projet. L'extracteur parcourt les unités de compilation et lit les membres directement via \texttt{bodyDeclarations()}, ce qui évite d'attribuer par erreur les membres des classes internes (telles que \texttt{LoanService.Loan}) à leur classe parente. 
\begin{itemize}
    \item \textbf{Nom, Paquetage et Genre :} Chaque type est identifié par son nom qualifié, son paquetage et son genre, classé parmi \texttt{CLASS}, \texttt{INTERFACE}, \texttt{ENUM} ou \texttt{RECORD}.
    \item \textbf{Hiérarchie (Bindings) :} La chaîne des superclasses et les interfaces directes ne figurant pas toujours explicitement sous forme de texte simple dans le code source, nous exploitons les liaisons sémantiques d'Eclipse JDT via \texttt{ITypeBinding.getSuperclass()} (en filtrant \texttt{java.lang.Object}) et \texttt{getInterfaces()}. Par exemple, la classe \texttt{LoanException} hérite correctement de \texttt{java.lang.Exception} et \texttt{java.lang.Throwable}.
    \item \textbf{Attributs et Méthodes :} Les attributs sont collectés avec leur nom, leur type déclaré et leur visibilité normalisée (\texttt{public}, \texttt{protected}, \texttt{private} ou \texttt{package}). Les méthodes sont quant à elles extraites avec leur nom, le nombre de paramètres et un indicateur booléen précisant s'il s'agit d'un constructeur.
\end{itemize}

\begin{lstlisting}[style=code, caption={Extrait de la méthode d'extraction de StructureExtractor}]
public class StructureExtractor {
    public List<TypeInfo> extract(List<ParsedFile> files) {
        List<TypeInfo> result = new ArrayList<>();
        for (ParsedFile file : files) {
            CompilationUnit unit = file.unit();
            String packageName = unit.getPackage() == null
                    ? ""
                    : unit.getPackage().getName().getFullyQualifiedName();
            for (Object element : unit.types()) {
                if (element instanceof AbstractTypeDeclaration type) {
                    addType(type, packageName, result);
                }}} return result;}}
\end{lstlisting}

L'exécution de notre analyseur sur le projet de validation valide l'ensemble des compteurs et critères attendus :

\begin{center}
\begin{tabular}{lr}
\toprule
\textbf{Élément} & \textbf{Nombre} \\
\midrule
Types totaux & 15 \\
\quad Classes & 12 \\
\quad Interfaces & 2 \\
\quad Énumérations & 1 \\
Méthodes totales & 59 \\
\quad Dont constructeurs & 9 \\
Attributs retenus & 21 \\
\bottomrule
\end{tabular}
\end{center}

De plus, l'analyse fine de certains composants clés confirme l'exactitude de nos structures de données :
\begin{itemize}
    \item \textbf{LoanService et sa classe interne :} \texttt{LoanService} possède 6 méthodes et 5 attributs, tandis que sa classe membre \texttt{LoanService.Loan} possède 4 méthodes et 3 attributs distincts.
    \item \textbf{Visibilités des attributs de \texttt{Item} :} L'analyse extrait fidèlement les visibilités hétérogènes, telles que \texttt{MAX\_LOAN\_DAYS} (\texttt{public}), \texttt{id} et \texttt{title} (\texttt{protected}), \texttt{borrower} (\texttt{private}), ainsi que \texttt{category} (\texttt{package}).
\end{itemize}

\begin{figure}[H]
    \centering
    \includegraphics[width=1\linewidth]{a2.png}
    \caption{Extrait du terminal validant l'extraction pour \texttt{LoanException} et \texttt{LoanService}}
    \label{fig:a2_results}
\end{figure}

\subsection{A3 — Extraction des appels et du type statique du receveur}

Pour analyser les interactions entre les classes et méthodes du projet, nous avons implémenté la classe \texttt{CallsExtractor}. Cette composante visite les nœuds de type \\ \texttt{MethodInvocation} et \texttt{SuperMethodInvocation} afin d'identifier l'ensemble des appels de méthodes effectués dans le code source.

L'extraction repose sur plusieurs mécanismes sémantiques offerts par l'AST d'Eclipse JDT :
\begin{itemize}
    \item \textbf{Résolution de la cible :} La méthode ciblée est obtenue grâce à \\ \texttt{resolveMethodBinding().getMethodDeclaration()}, ce qui permet de remonter de la spécialisation à la déclaration générique (par exemple pour l'appel à \texttt{List.add(Item)}).
    \item \textbf{Type statique du receveur :} Il est extrait via \texttt{getExpression().resolveTypeBinding()}. Pour un appel \texttt{super}, il s'agit de la superclasse de l'objet courant, tandis que pour un appel sans expression explicite, le contexte de la méthode propriétaire est utilisé. Par exemple, l'appel \texttt{loanable.checkOut(member)} conserve le receveur statique \texttt{library.model.Loanable} et non son implémentation concrète.
    \item \textbf{Catégorisation des appels :} Les appels sont classés dynamiquement en comparant leur cible avec l'ensemble des méthodes du projet collectées au préalable : \texttt{INTERNAL} (méthodes internes), \texttt{EXTERNAL} (bibliothèques tierces ou Java standard) ou \texttt{UNRESOLVED} en cas de dépendances manquantes.
\end{itemize}

\begin{lstlisting}[style=code, caption={Extrait de la méthode d'extraction des appels dans CallsExtractor}]
private void extractCallsFromMethod(
        MethodDeclaration method,
        ITypeBinding owner,
        CompilationUnit unit,
        Set<String> projectMethods,
        List<MethodCallInfo> calls) {

    if (method.getBody() == null) {
        return; }

    String caller = methodId(method, owner);
    method.getBody().accept(new ASTVisitor() {
        @Override
        public boolean visit(MethodInvocation invocation) {
            calls.add(createCall(
                    caller,
                    invocation.getName().getIdentifier(),
                    invocation.getExpression(),
                    invocation.resolveMethodBinding(),
                    owner,
                    unit,
                    invocation,
                    false,
                    projectMethods));
            return true; }

        @Override
        public boolean visit(SuperMethodInvocation invocation) {
            calls.add(createCall(
                    caller,
                    invocation.getName().getIdentifier(),
                    null,
                    invocation.resolveMethodBinding(),
                    owner,
                    unit,
                    invocation,
                    true,
                    projectMethods));
            return true; }});}
\end{lstlisting}

L'exécution de l'analyseur sur le projet de validation valide l'intégralité des indicateurs quantitatifs du point de contrôle :
\begin{center}
\begin{tabular}{lr}
\toprule
\textbf{Type d'appel} & \textbf{Nombre} \\
\midrule
Total des appels détectés & 94 \\
\quad Interne (du projet) & 46 \\
\quad Externe (bibliothèques) & 48 \\
\quad Non résolus & 0 \\
\bottomrule
\end{tabular}
\end{center}

Plusieurs cas particuliers confirment la robustesse de notre extracteur :
\begin{itemize}
    \item \textbf{Héritage et redéfinition :} Les méthodes héritées sont correctement associées à leur classe déclaratrice d'origine (par exemple, dans \texttt{LibraryApp.main} à la ligne 32, l'appel \texttt{zadig.getTitle()} a pour receveur \texttt{library.model.Book} et cible la méthode \\ \texttt{library.model.Item\#getTitle()}).
    \item \textbf{Contextes complexes :} L'analyse gère sans difficulté les blocs spécifiques comme l'appel explicite \texttt{super.describe()} dans \texttt{Book.describe} ou les expressions imbriquées au sein de lambdas (ex: \texttt{item.isAvailable()} dans \texttt{Catalog.available}).
\end{itemize}

\section{PARTIE B - Exploiter les faits : métriques et graphe d'appel}

\subsection{B1 - Un modèle de faits indépendant de JDT}

Pour satisfaire le principe de découplage, la phase d'exploitation des faits (métriques et graphes) ne manipule plus directement les structures de l'API Eclipse JDT. Nous avons conçu un modèle de faits orienté objet pur, représenté par le record global \texttt{ProjectFacts} ainsi que des structures associées situées dans le paquetage \texttt{hai913i.tp1.model} :
\begin{itemize}
    \item \texttt{ProjectFacts} : centralise l'ensemble des faits du projet (types, appels et nombre total de lignes de code). Son constructeur compact garantit l'immutabilité des collections via \texttt{List.copyOf()}.
    \item \texttt{TypeInfo} : encapsule le nom qualifié, le paquetage, le genre (\texttt{CLASS}, \texttt{INTERFACE}, \texttt{ENUM}), ainsi que les listes de superclasses, d'interfaces, d'attributs et de méthodes.
    \item \texttt{AttributeInfo} : stocke le nom, le type déclaré et la visibilité de l'attribut.
    \item \texttt{MethodInfo} : caractérise chaque méthode par son nom, son nombre de paramètres et un indicateur de constructeur.
    \item \texttt{MethodCallInfo} : modélise chaque appel en associant l'appelant (\textit{caller}), le nom de la méthode appelée, le type statique du receveur, la cible résolue, la ligne et le statut de résolution (\texttt{INTERNAL}, \texttt{EXTERNAL}, \texttt{UNRESOLVED}).
\end{itemize}

\subsubsection{Architecture et construction par \texttt{FactsExtractor}}
La classe \texttt{FactsExtractor} coordonne la construction de ce modèle en une seule passe à partir de toutes les unités de compilation. Les listes sont triées et copiées pour produire des sorties déterministes. Les étapes B2 et B3 travaillent ainsi sur un modèle stable, totalement indépendant de l'implémentation de JDT.

\begin{lstlisting}[style=code, caption={Extraction globale des faits avec FactsExtractor}]
public final class FactsExtractor {
    public ProjectFacts extract(List<ParsedFile> files) {
        StructureExtractor structureExtractor = new StructureExtractor();
        CallsExtractor callsExtractor = new CallsExtractor();
        return new ProjectFacts(
                structureExtractor.extract(files),
                callsExtractor.extract(files),
                countApplicationLines(files));
    } }
\end{lstlisting}

\subsubsection{Choix de conception}
\begin{itemize}
    \item \textbf{Gestion des surcharges (\textit{overloading}) et classes imbriquées :} Les identifiants uniques des méthodes et des appelants sont normalisés sous la forme d'une chaîne combinant la classe propriétaire et la signature des paramètres \\ (ex: \texttt{paquetage.Classe\#methode(TypeParam)}). \\ Cela permet de distinguer sans ambiguïté les méthodes surchargées ainsi que les classes membres (comme \texttt{LoanService.Loan}).
    \item \textbf{Déterminisme et reproductibilité :} Conformément aux recommandations, toutes les collections de faits sont explicitement triées. Ainsi, deux exécutions successives sur le même projet produisent une sortie strictement identique, sans effet de bord lié à l'ordre d'énumération du système de fichiers.
\end{itemize}

\subsubsection{Validation du modèle}
L'interrogation de ce modèle indépendant permet de retrouver à l'identique l'ensemble des chiffres clés validés en partie A (les 15 types, 59 méthodes, 21 attributs et 94 appels), confirmant que la rupture de dépendance avec JDT est totale et fonctionnelle.

\begin{figure}[H]
    \centering
    \includegraphics[width=1\linewidth]{b1.png}
    \caption{Exécution du JAR validant le modèle de faits indépendant et les compteurs d'appels}
    \label{fig:b1_validation}
\end{figure}

\subsection{B2 — Métriques}

À partir du modèle de faits indépendant construit en partie B1, nous avons implémenté la classe \texttt{MetricsCalculator} pour évaluer l'ensemble des indicateurs quantitatifs et qualitatifs du projet. Cette étape exploite exclusivement \texttt{ProjectFacts}, sans aucune dépendance envers Eclipse JDT.

\texttt{MetricsCalculator} applique la règle de calcul des percentiles par arrondi supérieur $k = \lceil n/10 \rceil$ (implémentée via \texttt{(n + 9) / 10}), inclut les égalités avec la valeur limite et exclut les entités de valeur nulle des classements. Le nombre de lignes d'un corps est calculé entre la ligne de l'accolade ouvrante et celle du dernier caractère du corps. De plus, toutes les collections de résultats sont explicitement triées par nom qualifié ou par signature, garantissant une reproductibilité stricte.

\subsubsection{Résultats et Point de Contrôle B2}
Les indicateurs quantitatifs globaux et les analyses avancées validés par l'exécution de l'analyseur sont résumés dans le tableau ci-dessous :

\begin{center}
\small
\begin{tabular}{p{0.64\textwidth}r}
\toprule
\textbf{Question / Indicateur} & \textbf{Résultat} \\
\midrule
Q1 -- Nombre de types / classes & 15 \\
Q2 -- Lignes physiques Java (LOC) & 493 \\
Q3 -- Nombre total de méthodes & 59 \\
Q4 -- Nombre total de paquetages & 4 \\
Q5 -- Nombre moyen de méthodes par classe & 3,93 \\
Q6 -- Moyenne de lignes par méthode avec corps & 4,64 (246 / 53) \\
Q7 -- Nombre moyen d'attributs par classe & 1,40 \\
Q8 -- Top 10\,\% classes par méthodes ($k=2$) & \texttt{Item} (10), \texttt{Member} et \texttt{LoanService} (6) \\
Q9 -- Top 10\,\% classes par attributs & \texttt{Item} et \texttt{LoanService} (5) \\
Q10 -- Intersection des deux tops & \texttt{Item} et \texttt{LoanService} \\
Q11 -- Classes avec plus de $X$ méthodes & $X=5$ : \texttt{Item}, \texttt{Member}, \texttt{LoanService} \\
& $X=4$ : 6 classes \\
Q13 -- Paramètre maximal & 5, \texttt{Statistics\#report} \\
\bottomrule
\end{tabular}
\end{center}

Pour la question Q12 (top 10\,\% des méthodes par classe selon les lignes de code), \texttt{Item\#checkOut} comporte 7 lignes et \texttt{LoanService\#borrow} en comporte 14. Les six méthodes de \texttt{Member} possèdent 3 lignes chacune. Enfin, les méthodes de \texttt{Loanable} n'ont pas de corps et ne sont donc pas sélectionnées.

\begin{figure}[H]
    \centering
    \includegraphics[width=0.9\linewidth]{b2.png}
    \caption{Exécution de la commande \texttt{metrics} validant les résultats de la partie B2}
    \label{fig:b2_metrics_output}
\end{figure}

\subsection{B3 — Graphe d'appels}

À partir du modèle de faits indépendant construit en B1, nous avons implémenté la classe \texttt{CallGraph} pour construire, structurer et exploiter le graphe d'appels orienté du projet.

\subsubsection{Stratégie de construction et sémantique}
Le graphe est modélisé selon les principes suivants :
\begin{itemize}
    \item \textbf{Nœuds :} L'ensemble de toutes les méthodes déclarées dans le projet (59 nœuds), identifiées par leur signature normalisée.
    \item \textbf{Arcs pondérés :} Chaque arc représente une relation d'appel interne entre une méthode appelante et une cible. Le poids de l'arc correspond au nombre de sites d'appel regroupés pour ce même couple.
    \item \textbf{Gestion des appels non internes :} Les appels externes et non résolus sont comptabilisés pour les métriques globales du système sans pour autant créer de nœuds fantômes ou d'arcs superflus dans le graphe.
    \item \textbf{Sortie exploitable et format textuel :} Le format texte en console a été privilégié pour sa simplicité de mise en œuvre, sa lisibilité directe sous forme de liste d'arcs pondérés, et son intégration transparente avec le système d'excédent de la ligne de commande du projet.
    \item \textbf{Analyse d'impact bidirectionnelle :} Le graphe maintient deux index internes (\texttt{outgoing} pour les appelées et \texttt{incoming} pour les appelantes), permettant de réaliser des requêtes d'analyse d'impact instantanées dans les deux sens pour n'importe quelle méthode.
\end{itemize}

\subsubsection{Résultats et Point de Contrôle B3}
L'analyse de validation exécutée par notre outil confirme les résultats attendus du TP :
\begin{itemize}
    \item \textbf{Indicateurs globaux :} 59 nœuds, 44 arcs distincts, 46 sites d'appel internes, 48 appels externes, et 0 appel non résolu.
    \item \textbf{Cas particuliers :} L'arc \texttt{Catalog\#add(Item,Item) $\to$ Catalog\#add(Item)} possède un poids de 2 ; la méthode \texttt{TextUtils\#repeat} contient une arête de récursion vers elle-même.
    \item \textbf{Requêtes d'appelantes (Analyse d'impact) :} \texttt{Loanable\#checkOut(Member)} n'est appelée que par \texttt{LoanService\#borrow}, tandis que \texttt{Item\#checkOut(Member)} n'a aucune méthode appelante recensée dans le graphe interne.
\end{itemize}

\begin{figure}[H]
    \centering
    \includegraphics[width=1\linewidth]{b3.png}
    \caption{Génération et affichage du graphe d'appels et de ses indicateurs}
    \label{fig:b3_graph_output}
\end{figure}

\subsubsection{Architecture, Validation et Synthèse du Projet}

La structure globale du projet respecte une séparation stricte des responsabilités entre le chargement, l'extraction des faits, le calcul des métriques, la modélisation du graphe et l'interface utilisateur.

\begin{center}
\small
\begin{tabular}{ll}
\toprule
Partie & Composants principaux \\
\midrule
A0 & \code{parse/ProjectSources}, \code{parse/JdtParser}, \code{Main} \\
A1 & \code{debug/PrintAst} \\
A2 & \code{extract/StructureExtractor}, \code{model/TypeInfo} \\
A3 & \code{extract/CallsExtractor}, \code{model/MethodCallInfo} \\
B1 & \code{extract/FactsExtractor}, \code{extract/MethodIds}, \code{ProjectFacts} \\
B2 & \code{metrics/MetricsCalculator}, \code{metrics/MetricsReport} \\
B3 & \code{graph/CallGraph}, \code{graph/CallEdge} \\
B4 & \code{cli/CliParser}, \code{report/TextReport}, \code{Main} \\
\bottomrule
\end{tabular}
\end{center}

\subsection{B4 — Un outil utilisable, robuste et reproductible}

Pour garantir l'ergonomie, la robustesse et la maintenabilité de notre analyseur, l'entrée de l'outil (\texttt{Main}) orchestre l'exécution tout en déléguant aux composants spécialisés. 

\subsubsection{Commandes et Reproductibilité}
Le projet est entièrement géré via Maven. Depuis une copie fraîche du dépôt et sans nécessiter d'EDI, les commandes principales suffisent à compiler, tester et exécuter l'outil :

\begin{lstlisting}[style=code]
mvn clean test
mvn clean package
java -jar target/hai913i-tp1-analyzer.jar ../resources/validation --action all 
--threshold 5
\end{lstlisting}
De plus, l'utilisation native des chemins de fichiers Java permet l'exécution sur des chemins contenant des espaces. Les tests présents vérifient le parsing JDT et la résolution de bindings entre plusieurs fichiers, le calcul des métriques et du graphe à partir du modèle, ainsi que le parsing des options de commande.

\subsubsection{Gestion robuste des erreurs et cas limites}
Conformément aux exigences de robustesse, l'outil gère les anomalies sans propager de piles d'exceptions brutes (\textit{stack traces}) et retourne un code de sortie non nul ($> 0$) :
\begin{itemize}
    \item \textbf{Paramètres invalides :} Un argument incorrect, un seuil absent ou mal formé ($X = abc$), un chemin inexistant ou un dossier sans fichier Java déclenchent un message explicite sur la sortie d'erreur et un code de sortie non nul.
    \item \textbf{Tolérance aux erreurs de compilation partielles :} Les erreurs de syntaxe ou de liaison sont signalées individuellement sur les fichiers concernés (comme dans le cas de \texttt{Broken.java} ou des exemples \texttt{missing-dep} / \texttt{syntax-error}), mais l'outil ne s'arrête pas brutalement et poursuit l'analyse sur le reste du projet valide.
\end{itemize}

\begin{figure}[H]
    \centering
    \includegraphics[width=1\linewidth]{b4.png}
    \caption{Gestion robuste des erreurs et affichage d'un message}
    \label{fig:b4_robustness}
\end{figure}

\section{Difficultés, Regard critique et Conclusion}

\subsection{Difficultés et limites techniques}
L'implémentation de cet analyseur statique a soulevé plusieurs défis techniques inhérents à l'analyse de code source Java :
\begin{itemize}
    \item \textbf{Résolution des liens (\textit{bindings}) :} L'utilisation d'Eclipse JDT Core exige une configuration rigoureuse du classpath pour éviter les résolutions incomplètes, en particulier pour les appels polymorphes ou l'héritage inter-classes.
    \item \textbf{Limites de l'analyse statique :} Le graphe d'appels repose sur une approximation structurelle (statique) sans résolution complète du polymorphisme dynamique (analyse CHA ou RTA poussée), ce qui peut introduire des imprécisions en présence de redéfinitions de méthodes complexes. De plus, l'analyse ne prend pas en compte les mécanismes dynamiques de la réflexion (\textit{reflection}).
    \item \textbf{Pertinence des métriques :} Les métriques orientées objet calculées (telles que le WOC ou le NOM) offrent une bonne vision de la complexité structurelle, mais restent limitées pour évaluer la qualité globale de la conception sans l'associer à des critères de couplage et de cohésion plus profonds.
\end{itemize}

\subsection{Utilisation des outils d'intelligence artificielle}
Conformément aux directives du module, un assistant IA (modèle Gemini) a été utilisé de manière collaborative pour :
\begin{itemize}
    \item La structuration et la rédaction des sections textuelles du rapport sous format LaTeX.
    \item L'harmonisation des options de la ligne de commande et la mise en forme de la documentation (README et rapport).
    \item L'aide au diagnostic des messages d'erreur et à la mise en place de la robustesse dans la classe \texttt{Main}.
\end{itemize}
\textbf{Vérification et responsabilité :} Toutes les suggestions de l'IA ont été systématiquement vérifiées, testées et validées par l'exécution de commandes dans le terminal et l'analyse du code source. Les choix architecturaux finaux, l'implémentation du modèle de faits et la validation des points de contrôle demeurent entièrement de notre fait.

\subsection{Pistes d'amélioration}
Pour de futures versions, plusieurs améliorations seraient bénéfiques :
\begin{itemize}
    \item Intégrer une hiérarchie de types plus fine pour l'expansion CHA (\textit{Class Hierarchy Analysis}) dans le graphe d'appels.
    \item Permettre l'exportation native du graphe d'appels aux formats standardisés (DOT / GraphML) pour une visualisation graphique externe.
    \item Étendre le jeu de métriques à des mesures de couplage entre paquetages (CBO, RFC).
\end{itemize}

\subsection{Conclusion}
Ce projet a permis de concevoir un analyseur statique robuste, indépendant de JDT dans sa logique métier, et parfaitement reproductible via Maven. Les différents points de contrôle (B1, B2, B3 et B4) ont été validés avec succès, démontrant la viabilité d'une architecture modulaire et testable.

\subsection{Références}
\begin{itemize}
    \item[\textbullet] Documentation officielle d'Eclipse JDT Core (ASTParser et CompilationUnit).
    \item[\textbullet] Supports de cours et notes du module HAI913I, Université de Montpellier.
    \item[\textbullet] Documentation Apache Maven (gestion du cycle de vie et packaging des livrables).
\end{itemize}

\end{document}
