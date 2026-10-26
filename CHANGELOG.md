# 🧾 CHANGELOG – Yūrei : L’Esprit du Dragon Perdu

> Suivi des évolutions du projet **Yūrei**, jeu d’exploration poétique et contemplatif développé pour Android (Jetpack Compose – Kotlin).  
> Ce document suit les conventions de versionnement sémantique : `MAJEUR.MINOR.PATCH`.

---

## [Non publié] - 2026-10-26

### Objectif porté à 30 âmes

- Chronomètre : décompte du temps monotone actif complet, indépendant du pas physique plafonné à 50 ms. Les ralentissements n’allongent plus les trois minutes ni la protection après collision. Le reliquat est décompté avant pause ou sortie ; le temps passé en pause est exclu. Une échéance dépassée est traitée avant toute nouvelle collecte. Les animations restent liées au temps de simulation pour éviter les sauts.

- Échelle commune : simulation dans un terrain logique de 1080 × 1920 unités, rendu uniforme centré sans déformation. Corps, vitesse, âmes, collisions et marges des berges partagent désormais le même repère, quelle que soit la résolution ou le format de fenêtre. Les anciennes positions sauvegardées sont adaptées à ce terrain à la restauration ; les changements de fenêtre ultérieurs ne déforment plus la partie. Le joystick conserve ses dimensions tactiles en dp.

- Contrôles : joystick flottant au point de contact, zone neutre de 14 dp, repère de direction et poignée visuelle. Les petites corrections vers le centre ne retournent plus le dragon ; revenir au centre ou relâcher conserve le cap. Le joystick disparaît à la pause ou à la fin du contact.

- 30 âmes en trois minutes ; transitions visuelles à 10 et 20 collectes.
- Sauvegarde v4 : les parties en cours conservent leur compteur et les anciens souvenirs restent débloqués.

### Défi chronométré du niveau 1

- 20 âmes à collecter en 180 secondes, avec victoire immédiate à la dernière âme et écran d’échec à expiration.
- Avancée continue, direction contrôlée par glissement, virages progressifs et rebonds non punitifs sur les berges.
- Collision avec la queue : perte de 10 secondes et trois segments, protection de deux secondes et message visible.
- Compteurs de temps et d’âmes ; lumière liée au temps restant. Pause sans décompte.
- Sauvegarde v3 du défi, de la direction et de la protection ; compatibilité avec les anciens souvenirs.
- Validation : 34 tests unitaires core/data réussis, lint debug sans erreur, parcours Android jusqu’aux 20 âmes et au souvenir, restauration de sauvegarde et expiration sans reprise possible. Les tests de cycle de vie passent lors du contrôle ciblé ; une première exécution avait échoué sur une transition d’activité de l’émulateur.

## [Non publié] - 2026-10-23

### Premier monde : La lanterne offerte

- Trois chapitres jouables avec paysage nocturne, torii, brume, âmes et refuges lumineux ; introduction et souvenir final illustré.
- Chaque chapitre dure 60 secondes avec quatre âmes collectées, ou 100 secondes avec au moins une âme. Le parcours vise 3 à 5 minutes de simulation active ; les pauses et les périodes sans collecte peuvent prolonger cette durée.
- Régénération de lumière dans les refuges, transitions progressives et réserve minimale de 8 % pour éviter une extinction punitive dans ce premier monde.
- Ambiance originale synthétisée, effets et musique du souvenir inclus dans l’APK ; génération reproductible avec `tools/generate_lantern_audio.py`.
- Sauvegarde version 2 : chapitre, temps, offrandes, introduction et fin. Lecture des anciennes sauvegardes ; souvenir débloqué conservé indépendamment des nouvelles parties.
- Validation : tests unitaires, builds debug/release, lint sans erreur ; parcours Android automatisé en temps réel jusqu’au souvenir (184 secondes), restauration du souvenir et reprise à mi-parcours après arrêt forcé du processus. Vérification visuelle des écrans d’introduction, de jeu et de fin sur émulateur API 36.

### Sauvegarde de partie et lumière

- Sauvegarde JSON versionnée dans DataStore : dimensions, segments, lumière, score, âmes, brume, temps du monde et temporisations de lumière.
- Points de sauvegarde toutes les deux secondes de simulation, à la collecte, à la pause et au retour à l'accueil. Écritures atomiques ordonnées, indépendantes de la durée de vie du ViewModel.
- Bouton « Continuer la partie » ; restauration en pause sans consommation de lumière hors du jeu. « Quitter » conserve la partie, une nouvelle partie la remplace et la fin de partie l'efface.
- Validation du contenu et de la version ; message visible en cas d'échec de lecture ou d'écriture, sans blocage du démarrage d'une nouvelle partie.
- Lumière initiale à 80 %. Perte de 0,2 point de pourcentage par seconde pendant le mouvement et les huit premières secondes de repos, puis 0,8 point par seconde. Une âme restitue 12 points et ajoute un segment.
- Sous 15 % de lumière pendant huit secondes, perte d'un segment du corps en échange de 12 points de lumière. La tête seule peut s'éteindre. Ce premier réglage reste à affiner par des sessions jouées ; il ne garantit pas une durée de partie fixe.
- Ajout de six tests de lumière, trois tests de sérialisation et un test Android de persistance. Lecture vérifiée dans un nouveau processus après arrêt forcé sur l'émulateur API 36.

Une interruption brutale reprend le dernier point de sauvegarde effectivement écrit ; les changements plus récents peuvent être perdus. Aucun mécanisme ne peut terminer une écriture après la mort du processus.

### Stabilisation du moteur

- Annulation du Job de la boucle à chaque arrêt pour empêcher les boucles concurrentes après un redémarrage.
- Horloge monotone et pas temporel plafonné à 50 ms pour éviter les sauts de simulation après une longue interruption.
- Initialisation du premier monde après mesure de la surface ; redimensionnement des âmes, de la brume et du dragon sans perdre la progression en mémoire.
- Déplacements bornés à la surface, y compris sur les petites fenêtres ; rejet des entrées tactiles non finies et des déplacements en pause.
- Suivi du corps à chaque mise à jour temporelle, indépendant du nombre d'événements tactiles ; longueur initiale correctement matérialisée.
- Pause automatique au passage en arrière-plan ; reprise explicite après recréation de l'activité et actualisation du contrôleur de navigation conservé par le ViewModel.
- Le bouton Retour reprend depuis la pause au lieu de laisser une partie inaccessible derrière l'accueil.
- Configuration des tests JUnit et Android ; ajout de 12 tests unitaires du moteur et de 2 tests d'intégration Android.

Validation : tests unitaires, compilation debug et APK de tests, lint (avec avertissements), puis 3 tests instrumentés réussis sur émulateur Android 16/API 36 (dont le test de contexte préexistant).

La sauvegarde après destruction du processus et le rééquilibrage de la lumière ont ensuite été ajoutés (voir ci-dessus). Aucune mesure de performances sur appareil physique n'est incluse dans cette validation.

## [0.1.0] - 2025-10-05
### ✨ Première version du projet (prototype visuel)
- Création du **dépôt GitHub** : [`Arckanna/Yurei`](https://github.com/Arckanna/Yurei).
- Ajout du **`.gitignore Android`** et initialisation du projet sous **Android Studio Ladybug**.
- Mise en place de la configuration **Gradle Kotlin DSL** :
    - `minSdk = 29`, `compileSdk = 36`, `targetSdk = 34`.
    - Support complet de **Jetpack Compose** et **Material 3**.
    - Alignement sur **JDK 17**.
- Création du namespace `com.valerie.yurei`.

#### 📘 Documentation
- Ajout du **`README.md`** complet :
    - Présentation du concept, gameplay, univers visuel et sonore.
    - Description technique (Kotlin, Compose, MVVM).
    - Roadmap de développement.
- Intégration du **`cahierDesChargesYurei.pdf`** :
    - Définition artistique, technique et UX complète.
    - Architecture cible (core/ui/data/audio).
    - Planning prévisionnel et contraintes techniques.

#### 🌌 Prototype visuel
- Implémentation d’un **premier écran d’accueil immersif (`GameScreen`)** :
    - Rendu **Canvas Compose**.
    - Fond dégradé nocturne (bleu lunaire → indigo).
    - Brume animée (particules “FogBlobs”) avec halo central type **ensō**.
    - Animation fluide (respiration, rotation lente).
- Ajout d’une structure `ui/game` pour les futurs écrans du jeu.

#### ⚙️ Ajustements techniques
- Passage de `compileSdk` à **36** pour compatibilité avec `androidx.activity:1.11.0` et `core-ktx:1.17.0`.
- Alignement de `sourceCompatibility` et `targetCompatibility` sur **Java 17**.
- Correction de l’erreur `@Composable invocations can only happen...` (refactor de `FogBackground()`).

---

## 🔮 Prochaines versions prévues

### [0.2.0] - Prototype interactif
- Ajout du **GameLoop Compose** (gestion `deltaTime` + `update()` / `draw()`).
- Déplacement du dragon spectral via **contrôle tactile**.
- Animation de segments lumineux (prévisualisation du corps du dragon).
- Écran **menu principal** (bouton “Commencer” + transitions).

### [0.3.0] - Système de sauvegarde & audio
- Intégration de **Jetpack DataStore (JSON)** pour les préférences et la progression.
- Gestion audio : **SoundPool** (FX courts) + **ExoPlayer** (musiques).
- Effets de résonance lors de la collecte d’âmes.

### [0.4.0] - Beta fermée (Google Play)
- Optimisation mobile (poids, FPS, batterie).
- Tests UI / performance.
- Publication interne (Closed Track).

---

## 🪶 Historique des branches et versions

| Branche | Statut | Description | Version associée |
|:--|:--:|:--|:--:|
| **main** | 🟢 Stable | Branche principale destinée aux versions finales et démonstrations. | v0.1.0 → ... |
| **dev** | 🟣 Active | Développement continu, intégration de nouvelles fonctionnalités avant validation. | v0.1.x, v0.2.x |
| **feature/ui-game** | 🔧 En préparation | Implémentation du GameLoop, contrôles tactiles et gestion des états du dragon. | v0.2.0 |
| **feature/audio-system** | ⏳ À venir | Intégration des sons, musiques et effets réactifs à la lumière. | v0.3.0 |
| **hotfix/** | ⚡ Occasionnel | Corrections urgentes sur `main` après release (stabilité ou compatibilité). | selon besoin |

> 🔁 Flux Git recommandé :
> `feature/*` → merge dans `dev` → validation & tests → merge dans `main` → **nouvelle release taguée**.

---

## 🧑‍💻 Auteur
**Valérie Lecœur** – ~~~~
📍 France  
🔗 [GitHub – Arckanna](https://github.com/Arckanna)

---

> *“Même les âmes perdues laissent une trace.” – Yūrei*
