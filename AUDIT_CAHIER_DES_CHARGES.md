# Audit Yūrei — 23 octobre 2026

## Conclusion

Le projet constitue un prototype technique de collecte avec corps segmenté. Il possède une base exploitable, mais ne réalise pas encore le voyage contemplatif et narratif décrit dans le cahier des charges. La priorité est de construire une première expérience complète, avec un monde, une émotion, un souvenir et une sauvegarde, puis d'étendre le contenu.

Le thème japonais est convaincant dans l'illustration d'accueil examinée ; il reste très peu matérialisé dans le rendu du jeu lui-même.

## Périmètre et méthode

- Référentiel : `cahierDesChargesYurei.pdf`, 15 pages physiques, lu intégralement. Les pages citées ci-dessous sont celles du fichier, pas les numéros de sa table des matières.
- Inspection des modules app, core, ui, data et audio, des ressources, des configurations et des tests. Vérification visuelle de l'illustration d'accueil et de la page UX du PDF.
- Compilation debug, tentative de tests unitaires et analyse lint effectuées. Aucun code applicatif modifié.
- Pas de session jouée sur appareil ni de mesure CPU/GPU/RAM : les conséquences de gameplay ci-dessous sont déduites du code et doivent être reproduites sur appareil.
- La référence « meynaupede » mentionnée dans la demande n'est pas définie dans le document. Aucune règle propre à ce jeu ne lui est attribuée dans cet audit. Le cahier décrit explicitement un « Snake spirituel » et cite notamment Snake Pass.
- Les extensions annoncées comme futures et le cloud optionnel ne sont pas comptés comme des manquements du prototype.

## Matrice de conformité

| Exigence du cahier | État observé | Preuve / écart |
|---|---|---|
| Android 10+, Kotlin, Compose (p. 11) | Présent | minSdk 29, compile/targetSdk 36, Canvas et écrans Compose. |
| MVVM et modules core/ui/data/audio (p. 11–12) | Présent, perfectible | Découpage effectif. GameViewModel construit directement le dépôt et l'audio ; le moteur dépend de types Compose, ce qui limite l'isolation des tests. |
| Glisser pour déplacer le dragon (p. 7, 9) | Présent, incomplet | Déplacement relatif au doigt ; aucune limite de position ni vitesse contrôlée. |
| Collecte, croissance et lumière (p. 4) | Partiel | Collision tête/âme, ajout d'un segment, augmentation de lumière. Aucun éclairage spatial du décor. |
| Perte de corps liée à l'immobilité ou à l'ombre (p. 4) | Absent | Décroissance permanente ; aucune zone sombre ni mesure d'immobilité ; removeSegment() n'est jamais appelée. |
| Mondes émotionnels, fragments de mémoire, fin de monde (p. 4, 7–9) | Absent | Un seul monde aléatoire, aucun modèle de niveau ou de souvenir, aucune transition narrative. |
| Sessions de 3–5 minutes, difficulté douce (p. 9) | Non démontré, réglage contradictoire | Réserve initiale épuisée en environ 5 secondes sans collecte ; pas de progression de difficulté. |
| Aquarelle, estampes, calligraphie, dragon fluide (p. 8–9) | Partiel | Illustration d'accueil cohérente ; jeu rendu en cercles et lignes. Pas de silhouette de dragon ni de décor narratif en partie. |
| Brume et particules (p. 9, 12) | Partiel | Particules mobiles présentes ; brume en disques blancs, indépendante des actions du joueur. |
| Interface minimaliste sans texte en jeu (p. 9) | Écart | Score, FPS, boutons Pause et Quitter affichés en permanence. |
| Accueil, pause, fin de partie (p. 14–15) | Partiel | Accueil, pause et paramètres présents ; GameOver retourne immédiatement à l'accueil. |
| Musique et sons japonais (p. 9, 12) | Infrastructure seule | SoundPool et ExoPlayer présents ; aucun fichier OGG/MP3 dans les sources examinées. |
| Sauvegarde progression, paramètres, niveaux (p. 12) | Partiel | DataStore Preferences conserve seulement record et deux interrupteurs audio ; pas de sauvegarde de partie. |
| JSON et chiffrement local (p. 12, 15) | Non implémenté | Pas de sérialisation de GameState ni de chiffrement applicatif AES. JSON est présenté comme préféré, puis retenu dans la roadmap. |
| Sauvegarde à chaque pause d'activité (p. 13) | Absent | Pas de raccordement au cycle de vie Android ; pause manuelle sans sauvegarde d'état. |
| 60 FPS, RAM < 150 Mo, bundle < 100 Mo (p. 13) | À mesurer | APK debug : 39 627 666 octets, soit 39,6 Mo. Cela ne valide ni le bundle final, ni la RAM, ni les FPS. |
| Tests appareils/API/GPU, graphismes réduits (p. 13–14) | Non démontré / absent | Pas de rapport de mesures trouvé, pas d'option de qualité graphique. |
| Tests unitaires et UI (p. 14) | Insuffisant | Deux tests de modèle Android, aucun test métier ; compilation des tests unitaires en échec. |

## Écarts prioritaires et corrections proposées

### P1 — Règle de lumière incompatible avec l'expérience annoncée

Dans `ui/src/main/kotlin/com/valerie/yurei/ui/viewmodel/GameViewModel.kt:64`, la perte vaut `0.0001 × dt` avec dt en millisecondes, soit 0,1 par seconde. La lumière initiale vaut 0,5 : sans collecte, extinction en environ 5 secondes ; une réserve pleine tient environ 10 secondes ; une âme ajoute environ 2 secondes. Ce calcul ne représente pas la durée réelle d'une partie avec collectes.

La perte s'applique même en mouvement, puis déclenche un retour à l'accueil. La disparition progressive du corps n'est pas raccordée au jeu, malgré `Dragon.removeSegment()` dans `core/.../entity/Dragon.kt:83`.

Correction : définir explicitement les états mouvement/repos/ombre, une tolérance initiale, la perte de segments et une récupération lisible. Valider ensuite une session novice de 3–5 minutes.

### P1 — Le but narratif est absent

`World.kt` régénère des âmes indéfiniment et `GameContracts.kt` ne contient ni monde débloqué, ni fragment, ni progression narrative. Le joueur peut accumuler des points, mais ne peut pas retrouver un souvenir ni terminer un monde.

Correction : réaliser un premier monde complet, avec objectif de collecte distinct du score, souvenir visuel ou sonore et transition finale. La collecte seule ne suffit pas à vérifier cette exigence.

### P1 — Pause Android et restauration non prises en charge

`app/src/main/java/com/valerie/yurei/MainActivity.kt` ne raccorde pas le jeu à onPause/onStop ou à un observateur de cycle de vie. La coroutine du ViewModel n'est donc pas arrêtée explicitement au passage en arrière-plan. `GameViewModel.kt:183` arrête la boucle lors de la pause manuelle mais n'enregistre rien ; `GameRepository.kt:18` ne définit que trois préférences.

Conséquences attendues : perte de lumière en arrière-plan tant que le processus continue à tourner ; disparition de la partie après destruction du processus.

Correction : pause automatique, sauvegarde atomique d'un état restaurable (dragon, monde, lumière, progression) et reprise explicite. Vérifier écran éteint, retour accueil Android et destruction du processus.

### P1 — Risque de plusieurs boucles après « Recommencer »

`core/src/main/kotlin/com/valerie/yurei/core/engine/GameLoop.kt:16` lance une coroutine sans conserver son Job ; stop() change seulement un booléen. Le bouton Recommencer envoie Quit puis Start immédiatement. Si l'ancienne coroutine dort encore, elle retrouve running=true au réveil et continue avec la nouvelle.

Conséquences déduites : monde mis à jour plusieurs fois, lumière consommée plus vite, compteur FPS trompeur. Correction : conserver et annuler le Job, assurer qu'une seule boucle existe. Tester plusieurs redémarrages successifs.

### P1 — Taille initiale et déplacements fragiles

`GameViewModel.kt:31` initialise le monde à 1080×1920 avant que le Canvas transmette ses dimensions réelles. updateWorldSize() change les dimensions mais ne repositionne ni les âmes déjà créées ni le dragon. Sur une surface plus petite, le premier monde peut contenir des éléments hors écran.

Les déplacements (`GameViewModel.kt:149`) ajoutent directement les deltas sans contrainte. Le dragon peut sortir de la zone de jeu. Les segments suivent uniquement lors des événements tactiles, avec une interpolation fixe, et non à chaque pas temporel du moteur.

Correction : créer le monde après mesure, définir un espace logique adaptable, borner le dragon ou choisir un rebouclage explicite, et animer le suivi dans la boucle. Vérifier petits écrans et redimensionnement Chromebook.

### P1 — Vérification automatique non opérationnelle

`testDebugUnitTest` échoue dans `ExampleUnitTest.kt` sur les références JUnit. Le catalogue contient JUnit mais `app/build.gradle.kts` ne déclare pas testImplementation. Les dépendances des tests instrumentés et leur runner ne sont pas configurés non plus ; leur exécution n'a pas été tentée.

Correction : configurer les tests, puis couvrir lumière, collecte, redémarrage unique et sauvegarde/restauration. Le test « 2 + 2 = 4 » ne valide aucune règle du jeu.

### P2 — L'identité artistique s'arrête principalement à l'accueil

L'asset `ui/src/main/res/drawable/dragon_bg.webp` combine dragon, lanternes, cercle peint et texture aquarelle ; il convient à l'intention. `GameScreen.kt` dessine cependant des cercles bleus, des âmes dorées et des lignes droites sur un dégradé sombre. La tête n'est pas identifiable comme celle d'un dragon et aucun environnement ne porte encore une émotion.

Correction : décliner l'illustration en langage visuel jouable : silhouette reconnaissable, ruban/corps fluide, fond peint lisible, palette d'un premier monde et apparition d'un souvenir. Conserver le débogage FPS hors de l'interface destinée au joueur.

### P2 — Audio et finition de l'interface

Le gestionnaire ignore silencieusement les ressources audio absentes : les options existent mais aucun son n'est fourni. La fin de partie revient à l'accueil sans transition. Le thème personnalisé YureiTheme n'est pas utilisé par MainActivity, qui instancie les couleurs Material par défaut. Le titre utilise Shantell Sans, bien que Yuji Boku soit présent dans les ressources.

Correction : intégrer une ambiance et un son de collecte, unifier le thème, concevoir une transition de fin douce et vérifier contraste, lisibilité et gestes sur appareil.

## Résultats des vérifications

- `assembleDebug` : réussi, APK produit.
- `testDebugUnitTest` : échec de compilation sur JUnit ; aucun succès de tests à revendiquer.
- `lintDebug`, relancé séparément : réussi, avec avertissements. Certaines vérifications Navigation sont ignorées car incompatibles avec la version de lint ; le succès n'est donc pas une validation complète de la navigation.
- Lint signale également le placement de l'image d'accueil dans drawable sans qualification de densité.
- Tests instrumentés, AAB release, performances, batterie et compatibilité Adreno/Mali : non vérifiés.

## Clarifications nécessaires dans le cahier lui-même

1. Il exclut le jeu de score dans l'intention créative (p. 8) mais demande score sauvegardé et écran Score dans la partie technique (p. 11–14). Choisir si les points restent un indicateur interne ou un objectif visible ; l'intention narrative devrait guider l'arbitrage.
2. La compatibilité vise Android 10–14 p. 13 et 10–16 dans la roadmap p. 15. Fixer une matrice de validation unique.
3. L'immobilité punitive peut contrarier la contemplation : définir un délai de grâce, des zones de repos ou une récupération avant d'équilibrer la difficulté.
4. Nombre de mondes, fragments requis, critères de fin, comportement aux bordures et critères de validation tactile ne sont pas définis. Ces décisions sont nécessaires pour tester la conformité.
5. Pour préserver l'idée d'origine, ajouter une courte section précisant ce qui doit être repris de « meynaupede » et ce qui doit être transformé. Le document actuel exprime surtout une identité autonome de Snake narratif japonais.

## Ordre de réalisation recommandé

1. Stabiliser boucle, dimensions, pause Android et tests.
2. Définir puis implémenter la lumière, l'ombre et la perte progressive du corps.
3. Livrer un seul monde de 3–5 minutes avec souvenir final et restauration de partie.
4. Appliquer à ce monde la direction artistique de l'accueil et une première ambiance sonore.
5. Tester des sessions réelles et mesurer FPS, mémoire, batterie et tailles d'écran avant de multiplier les mondes.

La validation du prochain jalon devrait être une courte partie complète, compréhensible et restaurable, plutôt qu'un ajout isolé de menus ou d'effets.
