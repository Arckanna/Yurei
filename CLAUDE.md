# Yūrei — guide pour Claude

Jeu Android d'arcade douce dans un univers japonais : on guide l'esprit d'un dragon qui collecte des âmes lumineuses (lac des lanternes, jardin des promesses, sanctuaire des marées).
Projet personnel d'une étudiante (Mastère ingénierie logicielle) : explique tes choix, privilégie le code lisible aux astuces, et signale ce que tu n'as pas pu vérifier.

## Où en est le jeu (vérifié le 17/11/2026)
- **Ce qui existe** : un défi de type « serpent » chronométré. Le dragon avance en continu, le joueur l'oriente avec un joystick flottant ; chaque âme ajoute un segment. But : 30 âmes en 3 minutes. Toucher sa queue ou un danger du décor coûte 10 s et 3 segments, puis 2 s de protection. Les berges font rebondir sans pénalité.
  - Monde 1 (lac) : queue seule. Monde 2 (sakuras) : 3 pierres fixes. Monde 3 (marées) : 3 sceaux cycliques (calme 5 s → alerte 2 s → actif 2 s).
  - Chaque victoire débloque un souvenir illustré et le monde suivant.
- **Intention de départ : Meynaupède** (Fred & Ted, 1985, Amstrad CPC, publié dans Hebdogiciel), un jeu de serpent en labyrinthe. Règles observées dans la vidéo de Valérie :
  - une chenille guidée dans un **labyrinthe fermé** (« les jardins de Girard ») ; déplacement sur grille dans **4 directions** ;
  - des **pastilles posées dans les couloirs** : chacune fait grandir la chenille et rapporte 11 points ; le **tableau est fini quand elles sont toutes mangées** ;
  - se **mordre la queue = mort** ;
  - **bonus de temps** (viser moins d'1 min 30 par tableau) et 1 000 points par tableau passé ; score, record, numéro de stage.
  - À vérifier : les murs tuent-ils ? y a-t-il des vies ? la difficulté augmente-t-elle d'un stage à l'autre ?
- **Écart avec Yūrei aujourd'hui** : terrain ouvert sans murs, direction libre à 360° au joystick, âmes réapparaissant au hasard (pas un ensemble fixe à vider), chrono de 3 min qui fait perdre (au lieu d'un bonus), pénalité de temps au lieu de la mort, pas de score affiché. **Le labyrinthe est l'élément central de Meynaupède qui manque le plus.**
- **Nouvelle direction décidée le 17/11/2026 : voir `REGLES_LABYRINTHE.md`** (fait foi pour le gameplay à venir). En bref : labyrinthe sur grille, 4 directions par glissements, le mur arrête sans punir, 3 lanternes (vies) par tableau, chrono = simple bonus, 3 tableaux par monde, vitesse croissante par tableau ; les trois mondes existants deviennent des labyrinthes et gardent leurs dangers (pierres, sceaux). On reprend les mécaniques du genre, pas le nom, les graphismes ni les niveaux de Meynaupède. Tant que ces règles ne sont pas codées, le jeu fonctionne encore avec l'ancien défi décrit plus haut.
- Le README décrit encore un concept contemplatif plus ancien (lumière/ombre, émotions) qui ne correspond plus aux règles réelles : le code et `CHANGELOG.md` font foi.
- **Feuille de route** : l'ordre de réalisation du labyrinthe est au §6 de `REGLES_LABYRINTHE.md` (étape 1 : moteur de labyrinthe dans `:core`). `CAHIER_DES_CHARGES_EVOLUTION.md` (30/10/2026) reste la référence pour l'identité, la qualité et la maintenance ; sa valeur de « 30 âmes en 3 minutes » est remplacée par les règles du labyrinthe. Il recommandait de commencer par **J0, l'audit** (moteur, commandes, cycle de vie, sauvegarde, audio, accessibilité, perfs), avant la refonte d'interface (carte des mondes, carnet de souvenirs) et le monde 4. `AUDIT_CAHIER_DES_CHARGES.md` (23/10) est un audit plus ancien : beaucoup de ses P1 ont été corrigés depuis.

## Stack
- Kotlin 2.0.21, AGP 8.13, JDK 17, Jetpack Compose (Material 3), Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`)
- minSdk 29, compileSdk/targetSdk 36 (les `build.gradle.kts` font foi ; `CONTRIBUTING.md` est en retard)
- Sauvegarde : DataStore + kotlinx.serialization (JSON versionné) ; audio : SoundPool + ExoPlayer 2.19 (ancienne API, migration Media3 à planifier, pas à faire en passant)
- Tests : JUnit 4 (JVM) et tests instrumentés Compose dans `app/src/androidTest`

## Architecture (modules Gradle)
- `:core` — moteur : `engine/GameLoop` (un seul `Job`, pas physique plafonné à 50 ms, chrono sur le temps réel actif), `LightBalance`, `entity/Dragon`, `world/` : `World` (âmes, brume), `TimedChallenge` (règles communes : chrono, collecte, pénalités, direction), un fichier par monde (`LanternJourney`, `SakuraGarden`, `MoonShrine`), `HeadCollision`, `GameViewport`, `WorldBounds`. `maze/` : le futur jeu en labyrinthe, en Kotlin pur — `MazePlan` (plan en texte : `#` mur, `.` couloir, `o` âme, `D` départ), `MazeLevel`, `MazeRun` (toutes les règles d'un tableau), `MazeCatalog` (liste des tableaux, identifiants stables), `swipeDirection` (glissement → direction) ; branché seulement au prototype du lac (voir `:ui`). Pas de logique d'UI ici ; l'ancien code utilise seulement `Offset`/`Size` de Compose comme types géométriques. Il doit rester testable sur la JVM.
- `:ui` — écrans Compose (`screens/` : `HomeScreen`, `GameScreen`, `SettingsScreen`, décors `*Art.kt`), composants (`SteeringJoystick`, `PauseOverlay`), `navigation/RootNav`, `viewmodel/GameViewModel` (boucle de jeu, orchestration) + `GameContracts` (état `GameUiState`, intentions `GameIntent`). Prototype du labyrinthe, séparé de l'ancien défi et sans sauvegarde : `viewmodel/MazeViewModel` (+ `MazeUiState`), `screens/MazeScreen` (glissements, affichage), `screens/MazeArt` (décor du lac), route `Route.Maze`.
- `:data` — `SavedGame` (+ codec et validation), `GamePreferences`, `GameRepository`.
- `:audio` — `AudioManager`, `Sfx`, `MusicPlaybackState` ; sons dans `audio/src/main/res/raw/`.
- `:app` — `MainActivity` (cycle de vie) et tests instrumentés (`FirstWorldTest`, `SakuraLevelTest`, `MoonLevelTest`, `PersistenceTest`, `EngineLifecycleTest`, `SteeringJoystickTest`).
- Simulation dans un terrain logique de 1080 × 1920 unités, rendu mis à l'échelle sans déformation ; le joystick reste en dp.

## Dette connue (à ne pas aggraver)
- Les mondes sont choisis par numéro (`when (level)`, `if (ui.level == 3)`) dans `GameViewModel`, `World`, `GameScreen`, `LanternArt` et `SavedGame` ; la progression utilise un booléen par monde (`sakuraMemoryUnlocked`, `moonMemoryUnlocked`…). Le cahier (lot C1/C2) prévoit un catalogue de niveaux et une progression par identifiant : un nouveau monde ne doit pas ajouter une condition de plus partout sans en parler d'abord.
- `GameUiState` garde des champs hérités de l'ancien parcours (chapitres, offrandes, refuges, score, fps).
- Les textes des écrans sont écrits en dur dans le code, pas dans `strings.xml`.

## Points sensibles
- **Sauvegarde versionnée** (actuellement v7, voir `SavedGame.kt`) : tout nouveau champ = nouvelle version + lecture des anciennes sauvegardes + test dans `SavedGameTest`. Ne jamais casser une partie ni une progression existante.
- Un seul `GameLoop` actif à la fois (pause, reprise, recommencer, arrière-plan Android).
- La pause et la sauvegarde doivent restituer fidèlement l'état : chrono, protection, phases des sceaux, segments déployés.
- Sons et icône sont générés par les scripts Python de `tools/` (`generate_*_audio.py`, `generate_launcher_icon.py`) : modifie le script plutôt que le fichier généré. Python n'est pas installé sur ce poste : préviens si un script doit être relancé.
- Identité à préserver (cf. `CAHIER_DES_CHARGES_EVOLUTION.md` §2) : ambiance douce, danger lisible et évitable, pas de multiplication des boutons en jeu, fonctionnement hors ligne, pas de télémétrie.

## Commandes (Windows : `.\gradlew.bat`, sinon `./gradlew`)
- Tests unitaires JVM : `test` (ou `:core:test`, `:data:test`, `:audio:test` pour un module)
- Build + lint : `assembleDebug lintDebug`
- Tests instrumentés (émulateur démarré) : `connectedDebugAndroidTest`

## Git : branches et commits
- **Chaque nouvelle avancée se fait sur une nouvelle branche** nommée `yurei-AAAA-MM-JJ-mot1-mot2` (date du jour, un ou deux mots en minuscules sans accents décrivant le travail), par exemple `yurei-2026-11-17-audit-moteur`.
- Au début d'une tâche : vérifie `git status` (s'il reste des modifications non commitées, demande-moi quoi en faire), puis crée la branche à partir de `main` à jour (`git switch main`, `git pull`, `git switch -c yurei-…`). Si je demande de partir d'une autre branche, fais-le. Si le travail continue celui de la branche courante, demande-moi avant d'en créer une nouvelle.
- **Tu ne commites jamais** : pas de `git commit`, `git push`, `git merge`, `rebase`, `reset --hard` ni `stash` sans demande explicite. Je relis le code et je commite moi-même.
- En fin de tâche, donne-moi la liste des fichiers modifiés et propose un message de commit en français, court et au présent, avec un préfixe de type (`feat`, `fix`, `test`, `docs`, `refactor`, `chore`…), par exemple `fix(sauvegarde) : relit les parties v6`.
- Le flux `feature/* → dev` décrit dans `CONTRIBUTING.md` n'est plus utilisé ; les branches sont fusionnées dans `main` par pull request (par moi).

## Règles de travail
- Après une modification : lance au minimum les tests unitaires du module touché et `assembleDebug`, et dis-moi ce qui passe ou échoue (avec l'erreur). Les tests instrumentés demandent un émulateur : dis-moi s'ils n'ont pas été lancés.
- Toute règle de gameplay nouvelle ou modifiée a son test dans `core/src/test` ; utilise des valeurs ou une graine aléatoire fixées pour qu'il soit reproductible.
- Mets à jour `CHANGELOG.md` (section « Non publié », datée) et le README quand une règle de jeu change.
- Une tâche = un sujet : pas de refactorisation large ni de nettoyage « en passant » hors du périmètre demandé ; propose-le plutôt.

## À ne jamais faire
- Toucher aux clés de signature (`../KeyStore`, `*.jks`, `*.keystore`) ou à `local.properties`.
- Monter les versions de Kotlin, AGP, de la BOM Compose ou d'autres dépendances sans que ce soit la tâche demandée.
- Modifier les documents de conception PDF/DOCX (`cahierDesChargesYurei.pdf`, `Yurei_Conception_Technique_V1.pdf`, `diagrames.pdf`, et ceux de `D:\Android\Yurei`).
