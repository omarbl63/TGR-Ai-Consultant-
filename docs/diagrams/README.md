Diagrammes UML — Agent Administratif IA (Trésorerie Générale du Royaume)

Trois diagrammes indépendants, en français, fidèles à l'architecture et aux
fonctions de l'application. Chaque diagramme est fourni en source PlantUML
(.puml), en image PNG et en image vectorielle SVG.

1. Diagramme de cas d'utilisation
   - Source : 1-cas-utilisation.puml
   - Images : cas-utilisation.png / cas-utilisation.svg
   - Acteurs : Employé, Responsable (chef de service), Directeur, Administrateur,
     Assistant IA (LLM + RAG). Cas d'utilisation des espaces mobile et dashboard,
     avec les relations «include» / «extend».

2. Diagramme de classes
   - Source : 2-classes.puml
   - Images : classes.png / classes.svg
   - Modèle du domaine : Utilisateur, SoldeConge, Conversation, Message,
     DemandeAdministrative, AnalyseIA, Citation, EvenementDemande, PieceJointe,
     Notification, JournalAudit, DocumentReglementaire, énumérations, et la
     couche IA (OrchestrateurIA, ServiceLLM, ServiceRAG).

3. Diagramme de séquence
   - Source : 3-sequence.puml
   - Images : sequence.png / sequence.svg
   - Scénario : création d'une demande de congé via l'assistant (dialogue,
     collecte des dates, création, recherche RAG, analyse de conformité) puis
     validation humaine sur le tableau de bord.

Regénérer les images (nécessite Java) :
  java -jar plantuml.jar -tpng -charset UTF-8 *.puml
  java -jar plantuml.jar -tsvg -charset UTF-8 *.puml

Ou en ligne, sans installation : copier le contenu d'un fichier .puml dans
https://www.plantuml.com/plantuml
