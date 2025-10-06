# 🧾 CHANGELOG – Yūrei : L’Esprit du Dragon Perdu

> Suivi des évolutions du projet **Yūrei**, jeu d’exploration poétique et contemplatif développé pour Android (Jetpack Compose – Kotlin).  
> Ce document suit les conventions de versionnement sémantique : `MAJEUR.MINOR.PATCH`.

---

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
- Animation de segments lumineux (pré-visualisation du corps du dragon).
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

## 🧑‍💻 Auteur
**Valérie Lecœur** – Mastère Expert en Ingénierie Logicielle  
📍 France  
🔗 [GitHub – Arckanna](https://github.com/Arckanna)

---

> *“Même les âmes perdues laissent une trace.” – Yūrei*
~~~~