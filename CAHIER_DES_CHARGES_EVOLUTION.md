# Yūrei — Cahier des charges pour la suite

Version 1.0 · 30 octobre 2026 · Document de cadrage

## 1. Intention et statut

Faire évoluer les trois premiers mondes vers un jeu plus complet, lisible et facile à maintenir, sans perdre son identité japonaise, sa poésie ni le plaisir de guider le dragon.

**Décisions acquises :** conserver le thème et la direction artistique ; commencer par un audit ; améliorer l’interface ; préparer la maintenance ; enrichir le contenu au-delà de trois niveaux.

**Propositions de ce document :** carte des mondes, carnet de souvenirs, campagne de six puis neuf mondes, objectifs facultatifs de maîtrise. Elles constituent une base de travail à valider avant leur développement.

Ce document ne vaut pas audit complet ni certification de l’état actuel. Il s’appuie sur l’historique du projet et une lecture ciblée du code. Les résultats des anciennes sessions de tests devront être renouvelés sur la version de référence retenue. Aucun calendrier ferme ni budget n’est engagé à ce stade.

## 2. Identité à préserver

- Dragon spectral, âmes lumineuses, paysages japonais et récit porté par les souvenirs.
- Couleurs douces, lumière progressive, musique discrète et absence d’agression visuelle.
- Parties courtes avec un véritable défi de trajectoire et de timing.
- Danger compréhensible et évitable ; erreurs sanctionnées de manière lisible.
- Commande simple au doigt, sans multiplication des boutons pendant la partie.
- Fonctionnement hors ligne pour la campagne, les réglages et les souvenirs.

L’amélioration de l’interface doit porter sur sa hiérarchie, sa cohérence et sa facilité d’utilisation. Une refonte de l’univers graphique n’est pas demandée.

## 3. Base existante et points à examiner

| Domaine | Base observée | Conséquence pour la suite |
|---|---|---|
| Campagne | Lac des lanternes, jardin des promesses, sanctuaire des marées | Préserver leurs règles et les utiliser comme référence d’équilibrage |
| Défi | 30 âmes, limite de trois minutes, croissance du corps, pénalités | Mesurer le plaisir et la difficulté sur téléphone avant de modifier ces valeurs |
| Variété | Queue, pierres, sceaux temporisés | Introduire les mécaniques suivantes progressivement |
| Interface | Accueil à plusieurs boutons, introductions longues, réglages audio | Concevoir une navigation capable d’accueillir davantage de mondes |
| Architecture | Modules `core`, `ui`, `data`, `audio`, `app` | Conserver cette séparation, réduire les conditions par numéro de niveau |
| Sauvegarde | Partie en cours, progression, format versionné jusqu’à 7 | Prévoir des migrations explicites et des identifiants stables |
| Qualité | Tests du moteur, de la sauvegarde, du joystick et des mondes | Compléter les scénarios réels et automatiser les vérifications répétitives |
| Présentation | Icône adaptative, illustrations et musiques propres au jeu | Documenter leurs sources et leur génération |

Repères de code : `GameViewModel.kt`, `GameScreen.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `SavedGame.kt`, `GameRepository.kt`, les classes de mondes et les tests associés.

La présence de conditions dispersées selon le niveau et de champs de progression dédiés à chaque monde est un constat de structure. Elle n’implique pas à elle seule un dysfonctionnement ; elle augmente le travail nécessaire pour ajouter du contenu.

## 4. Lot A — Audit préalable

### Objectif

Établir un état vérifiable du jeu avant d’entreprendre la refonte de l’interface et l’extension de la campagne.

### Périmètre

1. **Moteur et règles** : mouvement, croissance, collisions, bornes du terrain, génération des âmes, délai de protection, fin de partie, synchronisation du temps réel et de la simulation.
2. **Commandes** : précision, zone morte, changements de direction, perte du contact tactile, confort droitier/gaucher et usage à une main.
3. **Cycle de vie** : pause, arrière-plan, verrouillage, interruption, destruction du processus et restauration.
4. **Sauvegarde** : intégrité, écritures concurrentes, anciennes versions, sauvegarde invalide, mise à jour de l’application, progression et souvenirs.
5. **Audio** : casque filaire et Bluetooth, interruptions système, réglages, transitions musicales et silence en arrière-plan.
6. **Interface et accessibilité** : lisibilité, zones tactiles, grands caractères, contraste, navigation de retour, explications des règles, distinction entre décor et danger.
7. **Performances** : temps des images, saccades, allocations, mémoire après plusieurs parties, chauffe, batterie et taille de l’APK.
8. **Maintenance** : responsabilités des classes, duplication, constantes de gameplay, dépendances, génération des ressources, reproductibilité des builds et couverture des risques par les tests.

### Méthode et livrables

- Fixer une branche et un commit de référence ; conserver les données utilisateur avant toute manipulation destructive.
- Exécuter compilation, tests et lint ; distinguer les résultats nouveaux des résultats historiques.
- Utiliser un émulateur, un téléphone physique de référence et, si disponible, un appareil plus modeste. Toute couverture manquante doit être indiquée.
- Tester au minimum : écran compact et écran large, texte agrandi, reprise après arrêt du processus, audio interrompu, nouvelle installation et mise à jour avec ancienne sauvegarde.
- Livrer un rapport priorisé : P0 blocage/perte de données, P1 problème majeur, P2 gêne significative, P3 finition.
- Chaque anomalie doit contenir un scénario, un résultat attendu, un résultat observé, une preuve, un impact et un critère de correction. Séparer anomalie reproduite, risque à vérifier et suggestion.
- Livrer une matrice appareils/scénarios, une liste des dettes techniques et un backlog ordonné.

**Critère de sortie :** aucun P0 ou P1 confirmé ne reste sans correction ou décision explicite ; les P2 ont une priorité et un lot de traitement. Ne pas lancer une réécriture générale sur la seule base d’intuitions.

## 5. Lot B — Améliorer l’interface sans changer le thème

### B1. Accueil et carte des mondes

- Donner la priorité à « Reprendre » lorsqu’une partie existe, avec le nom du monde concerné.
- Proposer des accès distincts à « Les mondes », « Souvenirs » et « Paramètres ».
- Remplacer l’empilement des boutons de niveaux par une carte ou une liste illustrée et défilante.
- Afficher pour chaque monde : nom, numéro, état verrouillé/disponible/terminé, souvenir retrouvé et objectif principal.
- Expliquer les conditions de déblocage sans révéler les souvenirs à l’avance.
- Confirmer le remplacement d’une partie en cours lorsqu’un autre monde est lancé. Une simple consultation de la carte ne doit pas modifier la sauvegarde.

**Acceptation :** les six premiers mondes restent accessibles sans chevauchement sur un petit écran ; aucune action principale n’est coupée lorsque le texte est agrandi ; le monde à poursuivre est immédiatement identifiable.

### B2. Apprentissage et lecture du danger

- Remplacer les longues introductions systématiques par une présentation courte : but, commande, nouveauté du monde.
- Garder une aide détaillée consultable à tout moment.
- Introduire chaque nouveau danger dans une situation simple avant de le combiner à d’autres.
- Associer forme, animation et couleur aux états des dangers ; le son doit rester facultatif pour comprendre une alerte.
- Rendre le temps, les âmes et les pénalités lisibles sans masquer la trajectoire.
- Expliciter la protection temporaire et le caractère décoratif de la lumière.

**Acceptation :** un nouveau joueur peut expliquer l’objectif et reconnaître le danger après l’introduction ; aucun danger ne nécessite uniquement la perception d’une couleur ou d’un son.

### B3. Pause, résultats et réglages

- Uniformiser typographie, couleurs, espacements et boutons sur tous les écrans.
- Conserver des commandes de pause, reprise et sortie prévisibles ; éviter les pertes de progression par erreur.
- En fin de partie, afficher la réussite ou l’échec, les âmes recueillies et une indication utile pour la prochaine tentative, sans jugement culpabilisant.
- Réserver la lecture du souvenir à un moment calme, après la réussite.
- Ajouter, après évaluation de leur utilité : volume musique/effets, réduction des animations, vibrations facultatives et réglage de sensibilité avec retour au réglage par défaut.
- Rendre les menus utilisables avec lecteur d’écran ; étudier séparément l’accessibilité de la conduite en temps réel, sans promettre une accessibilité complète non testée.

**Acceptation :** réglages persistants après redémarrage ; textes lisibles ; boutons confortables au doigt ; aucun retour de navigation ambigu.

### B4. Carnet de souvenirs

- Consulter et relire les souvenirs débloqués sans rejouer les niveaux.
- Présenter les souvenirs manquants sans divulguer leur contenu.
- Permettre de quitter la lecture et de couper sa musique simplement.
- Préserver les souvenirs acquis lorsqu’une partie est perdue ou recommencée.

## 6. Lot C — Maintenance et capacité d’évolution

### C1. Définition commune des mondes

Introduire progressivement un catalogue de niveaux : identifiant stable, ordre, prérequis, titre, objectifs, durée, paramètres des dangers, palette, ressources audio, introduction et souvenir.

Séparer les paramètres de contenu du comportement des mécaniques. Une mécanique nouvelle reste du code testé ; un niveau qui réutilise des mécaniques existantes doit pouvoir être déclaré sans multiplier les conditions dans les écrans, le ViewModel et le dépôt de données.

**Acceptation :** les trois mondes actuels passent par cette structure sans changement involontaire de règles ; un monde supplémentaire réutilisant des mécaniques existantes n’exige pas de modifier la logique générale de sauvegarde ou de navigation.

### C2. Responsabilités et sauvegardes

- Garder le moteur indépendant du rendu et des composants Android autant que possible.
- Clarifier la séparation entre état de simulation, état de navigation et état affiché.
- Stocker la progression par identifiant de monde plutôt que par nouveau booléen à chaque ajout.
- Distinguer partie en cours, progression permanente et préférences.
- Définir des migrations depuis les formats supportés ; ne jamais réinitialiser silencieusement une progression valide.
- Prévoir un traitement compréhensible des sauvegardes incompatibles ou invalides et préserver une copie récupérable avant toute conversion risquée.
- Tester notamment les phases des obstacles, l’état des segments et la cause des pénalités après restauration.

**Acceptation :** les sauvegardes de référence des anciens formats conservent niveaux, souvenirs et réglages ; une erreur de lecture ne provoque pas de crash ni d’effacement silencieux.

### C3. Qualité et livraison

- Automatiser compilation, tests pertinents et lint à chaque modification proposée à l’intégration.
- Conserver quelques scénarios instrumentés essentiels : première partie, progression, sauvegarde/restauration, victoire, échec et navigation.
- Utiliser des graines aléatoires contrôlées dans les tests de gameplay pour rendre les anomalies reproductibles.
- Ajouter une recette manuelle sur téléphone avant chaque version destinée aux joueurs.
- Mesurer les performances avant optimisation. Cible initiale à confirmer avec l’audit : fluidité proche de 60 images/s sur l’appareil de référence ; comportement correct et chronomètre juste en cas de ralentissement.
- Versionner les releases, tenir le journal des changements, documenter les commandes de build et les sources des ressources.
- Examiner les dépendances obsolètes, notamment l’audio, puis planifier les migrations utiles avec tests de non-régression.
- Garder les clés de signature hors du dépôt ; documenter la fabrication et la conservation des versions distribuées.

**Rythme proposé :** tri des retours à chaque livraison, contrôle des dépendances mensuel, recette des appareils et des sauvegardes avant chaque release. L’ampleur de la maintenance doit rester compatible avec la taille du projet.

## 7. Lot D — Étendre la campagne

### Trajectoire recommandée

**Premier palier : six mondes aboutis.** Les trois actuels constituent le premier chapitre. Ajouter trois mondes distincts après la consolidation technique et l’interface extensible.

**Deuxième palier : neuf mondes.** À engager après les retours sur les six premiers. Ne pas produire d’emblée neuf variations visuelles d’une même règle.

La durée actuelle de trois minutes est une limite, pas une durée garantie de contenu : un joueur peut terminer plus tôt. Six mondes ne représentent donc pas automatiquement dix-huit minutes de découverte. La durée réelle et l’intérêt des nouvelles tentatives devront être mesurés.

### Proposition de campagne

| Monde | Ambiance | Défi ou nouveauté proposée | Intention du souvenir |
|---|---|---|---|
| 1. Le lac des lanternes | Eau, brume, lumière chaude | Apprendre la collecte et éviter sa queue | Être guidé |
| 2. Le jardin des promesses | Sakura, printemps | Anticiper les obstacles fixes | Retrouver une promesse |
| 3. Le sanctuaire des marées | Lune, torii, nuit indigo | Choisir le moment de traverser les sceaux | Avoir un lieu où revenir |
| 4. La forêt des murmures | Bambous, verts profonds, vent | Traverser des couloirs larges qui se déplacent lentement, avec annonce de leur mouvement | Apprendre à écouter |
| 5. Le pont des érables | Automne, rivière, feuilles rouges | Composer avec des courants doux et visibles, dans des zones limitées | Accepter de changer de chemin |
| 6. Le jardin sous la neige | Neige, silence, lueurs bleues | Recueillir des âmes autour d’obstacles mobiles lents, sur des trajectoires prévisibles | Garder une présence dans l’absence |
| 7. La fête des lumières | Lanternes, village au crépuscule | Choisir un parcours entre groupes d’âmes disponibles à tour de rôle | Partager une joie |
| 8. La montagne des échos | Sommets, ciel nocturne | Combiner deux mécaniques déjà apprises, avec des moments de répit | Reconnaître le chemin parcouru |
| 9. L’aube du retour | Lumière du matin, motifs des mondes précédents | Épreuve finale progressive, sans règle surprise | Réunir les souvenirs |

Ces noms et règles sont des propositions, pas des engagements de production. Les courants du monde 5 devront être particulièrement testés : ils ne doivent pas donner l’impression que le joystick fonctionne mal.

### Règles de conception de chaque nouveau monde

- Une idée principale nouvelle au maximum ; la faire découvrir avant d’augmenter sa difficulté.
- Une différence de décision pour le joueur, au-delà d’un changement de couleur.
- Des zones de passage sûres, une visibilité suffisante et des objectifs atteignables sans attendre un tirage aléatoire favorable.
- Des collisions cohérentes avec les silhouettes affichées ; la lumière et les ornements restent indépendants de la zone de collecte.
- Un avertissement observable avant l’activation d’un danger et une protection compréhensible après une pénalité.
- Une sauvegarde complète des états nécessaires à une reprise fidèle.
- Un souvenir, une composition visuelle et une ambiance musicale propres au monde.
- Conserver 30 âmes et trois minutes comme référence de prototype. Toute variation doit être justifiée par les essais et annoncée au joueur avant le départ.

### Équilibrage et rejouabilité

- Observer des joueurs novices et habitués : compréhension, collisions, raisons des échecs, temps de réussite et envie de recommencer.
- Séparer les problèmes de commande des difficultés voulues.
- Ne pas imposer un taux de réussite cible avant d’avoir une première mesure ; consigner les critères choisis ensuite.
- Proposer des objectifs facultatifs après la première réussite : parcours sans collision, meilleur temps restant, collecte régulière.
- Ne pas conditionner l’accès aux mondes suivants à une performance parfaite.
- Envisager plus tard un mode promenade sans chronomètre, clairement séparé du défi principal et sans mélanger les records.

**Acceptation d’un monde :** cycle complet jouable et testé, victoire et échec cohérents, reprise après fermeture du processus, musique respectant les interruptions système, accès au souvenir, absence d’âme inaccessible et recette sur téléphone.

## 8. Ordre de réalisation et jalons

| Jalon | Travail | Livrable / condition de passage |
|---|---|---|
| J0 | Audit de la version de référence | Rapport, preuves, matrice de tests et backlog priorisé |
| J1 | Corrections prioritaires | Résolution des défauts bloquants et des régressions confirmées |
| J2 | Maquettes et parcours d’interface | Accueil, carte, pause, résultats, carnet et réglages validés sur petit écran |
| J3 | Socle de maintenance et nouvelle interface | Catalogue de niveaux, migrations testées, trois mondes actuels préservés |
| J4 | Prototype du monde 4 | Validation du nouveau défi sur téléphone avant production des suivants |
| J5 | Mondes 4 à 6 finalisés | Première campagne étendue, recette, équilibrage et version testable |
| J6 | Retours et maintenance | Ajustements documentés ; décision sur la suite et la rejouabilité |
| J7 | Mondes 7 à 9 | Extension conditionnée par la qualité et les retours du premier palier |

Les correctifs critiques passent avant les nouveautés. Les refactorisations doivent être progressives et contrôlées par les tests des trois mondes existants. Estimer charge et calendrier à la fin de J0, puis réviser après le prototype du monde 4.

## 9. Hors périmètre initial

Multijoueur, comptes obligatoires, sauvegarde cloud, boutique, publicités, classements en ligne, contenu quotidien et éditeur de niveaux accessible aux joueurs. Ces ajouts créeraient de nouvelles contraintes de maintenance avant que la campagne principale soit suffisamment étoffée.

Ne pas ajouter de collecte de données ou de télémétrie distante par défaut. Les premières mesures peuvent être réalisées par observation et journaux locaux de développement.

## 10. Principaux risques et mesures

| Risque | Mesure |
|---|---|
| Plus de niveaux, mais sensation de répétition | Exiger une décision de jeu différente et tester le prototype avant production |
| Interface encombrée | Carte de mondes et carnet séparés de la partie |
| Perte de progression pendant la refonte | Sauvegardes de référence, migrations et tests de mise à jour |
| Difficulté attribuée à tort aux commandes | Essais sur téléphone avec joueurs de niveaux différents |
| Refonte trop large qui retarde le jeu | Changements progressifs, bornés par les besoins du prochain monde |
| Dégradation de l’atmosphère | Respect de la palette, du rythme, du silence et de la lisibilité |
| Augmentation du poids et des saccades | Mesures par version, contrôle des ressources et recette sur appareil modeste |

## 11. Définition d’une version livrable

- Aucun blocage ni perte de progression connus sans décision explicite.
- Tests pertinents et lint exécutés avec résultats documentés.
- APK debug testable et compilation release vérifiée, ressources audio incluses.
- Recette réelle sur les appareils retenus ; limites de couverture signalées.
- Parcours complet : lancement, reprise, sélection, jeu, pause, victoire/échec, souvenir et retour à l’accueil.
- Les sauvegardes supportées restent lisibles ; une interruption ne modifie pas silencieusement les règles.
- Changements versionnés sur une branche dédiée, journal des changements et consignes de test disponibles.

## 12. Première action recommandée

Commencer par **J0 : l’audit complet**, sans lancer simultanément la refonte ni le niveau 4. À son issue, retenir les corrections urgentes et valider les parcours de la future interface. La cible de six mondes constitue le premier objectif de contenu proposé ; neuf mondes reste l’horizon suivant.
