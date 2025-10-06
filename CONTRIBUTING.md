# 🤝 Contribuer à Yūrei : L’Esprit du Dragon Perdu

Merci de votre intérêt pour **Yūrei** ! Ce document explique comment installer le projet, créer des branches, nommer vos commits/PR, lancer les tests, et ouvrir une issue propre.

---

## 🧰 Prérequis & installation

- **Android Studio** Ladybug+ (ou plus récent)
- **JDK 17**
- **Android SDK**: Android **16 (API 36)** installé
- **Gradle Wrapper** (fourni par le projet)
- **Git**

# Cloner le dépôt
git clone https://github.com/Arckanna/Yurei.git
cd Yurei

# Travailler sur la branche de dev
git checkout dev
Config projet actuelle

minSdk = 29, compileSdk = 36, targetSdk = 34

Kotlin + Jetpack Compose (Material 3)

Namespace : com.valerie.yurei

🌿 Flux Git
main → stable (releases & démos)

dev → développement actif

feature/<sujet> → nouvelles fonctionnalités

hotfix/<sujet> → correctifs urgents depuis main

Créer une branche de feature :

git checkout dev
git pull
git checkout -b feature/ui-game-loop
Ouvrir une PR : feature/* → dev
Après review & tests OK, dev → main pour la release.

✍️ Commits : Conventional Commits
Merci d’utiliser le format suivant :

<type>(scope?): <message au présent et concis>

Types courants

feat: nouvelle fonctionnalité

fix: correction de bug

docs: doc (README, CHANGELOG, etc.)

refactor: refactor sans changement de comportement

chore: maintenance (gradle, build, ci)

test: tests uniquement

perf: amélioration de performance

style: formatage, no-op

Exemples:

feat(game): add breathing fog background on GameScreen

fix(ui): prevent crash on null canvas size

chore(build): bump compileSdk to 36

docs: add CHANGELOG with branch history

🧪 Build, tests & qualité
Build / run
Ouvrir dans Android Studio → Run ‘app’ (AVD API 29+)

Ligne de commande :

./gradlew assembleDebug
./gradlew installDebug
Tests
bash
Copier le code
./gradlew test              # tests unitaires JVM
./gradlew connectedAndroidTest  # tests instrumentés (AVD requis)
Lint & format (optionnel mais recommandé)
Si tu ajoutes ktlint ou detekt plus tard :

./gradlew ktlintCheck ktlintFormat
./gradlew detekt

🧩 Style & architecture
Langage : Kotlin

UI : Jetpack Compose

Architecture : MVVM

Organisation des packages (objectif court terme) :

com.valerie.yurei
├─ core/     # boucle de jeu, time-step, collisions simples
├─ ui/       # écrans, HUD, transitions
├─ data/     # DataStore, modèles de sauvegarde
└─ audio/    # SoundPool/ExoPlayer (FX & musique)

Règles de base

UI déclarative, side-effects isolés (LaunchedEffect, remember…)

Pas d’accès direct à la persistance depuis l’UI (passer par ViewModel)

Composables purs autant que possible (stateless), préfixer les previews par @Preview

Favoriser des fonctions pures dans core pour la logique de jeu

✅ Checklist Pull Request
Avant d’ouvrir la PR :

 La PR cible dev

 Titre en Conventional Commit (ex: feat(game): add touch controls)

 Description claire (quoi/pourquoi/screens si UI)

 Build OK (assembleDebug)

 Tests OK (si applicable)

 Pas de warnings bloquants (lint/IDE)

 Pas d’API/permission ajoutée sans justification

Template PR suggéré

### Objectif
Décrire ce que fait la PR et pourquoi.

### Changements
- …

### Screens / Vidéo (si UI)
- …

### Tests
- …

### Divers
- Concerne l’issue #<num> (si applicable)
🐛 Ouvrir une issue
Merci de préciser :
Type: bug / feature / docs
Contexte: version d’Android Studio, API device/émulateur
Étapes pour reproduire (si bug)
Résultat attendu / observé
Logs (stacktrace, capture)
Labels (bug, enhancement, documentation, good first issue, help wanted)

🚀 Releases & tags
Mise à jour du CHANGELOG.md

Tag sémantique :
~~~~
git tag -a v0.1.0 -m "First prototype – GameScreen visual"
git push origin v0.1.0
Merge dev → main pour publier la version

🧾 Licence & attribution
© Valérie Lecœur. Tous droits réservés.
Merci de citer le projet si vous en réutilisez des idées (DA, concepts, snippets).