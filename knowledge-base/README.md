Base de connaissances administrative (corpus RAG)

Ce dossier contient le corpus documentaire utilisé par le système RAG de l'agent administratif AdminAI. Les documents simulent la documentation interne d'une administration publique marocaine et sont rédigés en français administratif.

⚠️ Avertissement — Ces documents constituent une simulation créée pour un Projet de Fin d'Études. Ils sont inspirés des pratiques administratives publiques marocaines courantes mais ne reproduisent aucun document officiel d'une institution réelle.

Organisation

Dossier  ·  Contenu
01-rh/  ·  Ressources humaines : manuel de l'employé, politique de congés.
02-missions/  ·  Ordres de mission et politique de déplacement.
03-finance/  ·  Remboursement des frais, règles budgétaires, guide finances.
04-procedures/  ·  Procédures administratives, circuits de validation, pièces justificatives.
05-organisation/  ·  Organigramme, guide des managers, FAQ.
06-modeles/  ·  Modèles de formulaires et exemples de décisions administratives.

Cohérence du corpus

Les documents partagent une terminologie et des règles cohérentes :
- Les grades et échelles de rémunération sont utilisés de manière uniforme.
- Les circuits de validation (employé → supérieur hiérarchique direct → directeur) sont identiques partout.
- Les délais, plafonds et taux sont non contradictoires entre les politiques.
- Chaque document référence les autres documents pertinents.

Ingestion

Ces fichiers sont destinés à être ingérés par le pipeline RAG (parsing, découpage en chunks de 800–1000 tokens avec 15–20 % de chevauchement, génération d'embeddings bge-m3, indexation dans pgvector).
