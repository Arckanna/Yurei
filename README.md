# 🌙 YŪREI – L’Esprit du Dragon Perdu  

> *« Errant entre la vie et la mort, un dragon d’esprit cherche la lumière oubliée.  
> Même les âmes perdues laissent une trace. »*

---

## 🎮 Présentation du projet

**Yūrei (幽霊)** est un jeu mobile **d’exploration poétique et contemplative**, développé pour **Android**.  
Le joueur y incarne **l’esprit d’un dragon déchu**, voyageant entre la vie et la mort pour retrouver les fragments de sa mémoire et restaurer son essence.  

Chaque monde exploré représente une **émotion** (tristesse, sérénité, colère, amour…) et offre une expérience sensorielle entre **brume, lumière et silence**.

---

## 🧩 Concept & Gameplay

- Le joueur contrôle un **dragon spectral** dans un environnement vaporeux.  
- Les **âmes lumineuses** collectées allongent le corps du dragon et illuminent son chemin.  
- Rester trop immobile ou sombrer dans l’ombre fait disparaître une partie de son corps.  
- Chaque monde correspond à une **émotion** et dévoile un **fragment de mémoire** du dragon.  
- Objectif : **retrouver son essence et l’équilibre entre lumière et obscurité.**

🎮 **Genre :** Exploration / Réflexion / Arcade contemplatif  
🕹️ **Inspiration gameplay :** Gris · Spiritfarer · Sky: Children of the Light · Snake Pass

---

## 🏮 Univers & Direction artistique

- **Style visuel :** aquarelle animée, estampes japonaises (ukiyo-e), calligraphie zen.  
- **Palette :** bleu lunaire · violet brume · blanc spectral · argent lumineux.  
- **Ambiance :** brume mouvante, particules de lumière, interface minimaliste.  
- **Direction sonore :** instruments traditionnels japonais (shakuhachi, shamisen, koto) mêlés à des nappes ambiantes méditatives.

> 🎨 *Chaque geste est une prière. Chaque lumière, une mémoire retrouvée.*

---

## 👥 Public cible

- Joueurs contemplatifs sensibles à la **narration visuelle et sonore**.  
- Public aimant les **expériences poétiques et immersives**.  
- Adapté aux **sessions mobiles courtes** (3–5 minutes).

---

## ⚙️ Technologies et Architecture

| Élément | Choix |
|:--|:--|
| **Plateforme** | Android 10+ |
| **Langage** | Kotlin |
| **Framework/UI** | Jetpack Compose |
| **Architecture** | MVVM (Model–View–ViewModel) |
| **Contrôle de version** | GitHub |
| **Persistance locale** | Jetpack DataStore (JSON) |
| **Audio** | SoundPool / ExoPlayer |
| **Tests** | JUnit, MockK, Compose UI Test |
| **Profiling & Debug** | Android Profiler, Layout Inspector |

---

## 📁 Structure du projet

yurei/
├── core/ # Logique du jeu, moteur, collisions, brume
├── ui/ # Écrans, menus, transitions (Jetpack Compose)
├── data/ # Sauvegardes, progression, paramètres
├── audio/ # Gestion des musiques et effets (SoundPool + ExoPlayer)
└── audio/src/main/res/raw/ # Sons WAV : ambiance, souvenir et effets

---

## 🔊 Fichiers audio inclus

Les ressources originales synthétisées sont incluses dans `audio/src/main/res/raw/` :
`soul_collect`, `pause`, `resume`, `game_over`, `button_tap`, `ambient` (boucle) et `memory` (souvenir final).
Elles sont reproductibles avec `python tools/generate_lantern_audio.py`, sans bibliothèque externe ni échantillon téléchargé.

Le premier monde, **La lanterne offerte**, comporte trois chapitres de 60 à 100 secondes avec collecte d’âmes, refuges régénérants et souvenir final. Compter 3 à 5 minutes hors pauses, avec au moins une collecte par chapitre ; une exploration sans collecte peut durer davantage. La progression est sauvegardée et restaurée en pause après fermeture du processus.

---

## 📱 Performances & Optimisation

| Objectif | Spécification |
|:--|:--|
| FPS cible | 60 fps |
| Mémoire max | < 150 Mo |
| Taille APK | < 100 Mo |
| Compatibilité | Android 10 → 14 (Adreno / Mali GPU) |
| Sauvegarde | Automatique à chaque pause |

---

## 🔮 Message & philosophie

> “La lumière ne naît pas du combat, mais du souvenir de ce qui fut.”

Yūrei invite à une 'introspection visuelle et sonore' appelant que la **fragilité** et la **lumière intérieure** sont les clés de la renaissance.

---

## 🧭 Feuille de route (prévisionnelle)

| Phase | Durée | Livrables |
|:--|:--:|:--|
| Conception technique | 1 semaine | Diagrammes MVVM, structure du projet |
| Prototype gameplay | 2 semaines | Mouvements, collisions, brume |
| UI et menus | 1 semaine | Écrans d’accueil, pause, score |
| Audio & FX | 1 semaine | Musiques, effets |
| Optimisation mobile | 1 semaine | Tests FPS, batterie |
| Beta fermée | 2 semaines | Feedback utilisateurs |
| Release Google Play | 1 semaine | APK final |

---

## 🧠 Auteur

👩‍💻 **Valérie Lecœur**  
Étudiante Mastère Expert en Ingénierie Logicielle 
Développeuse Android & Unity  
📍 France  
🔗 [GitHub – Arckanna](https://github.com/Arckanna)

---

## 🪶 Licence

Ce projet est protégé par le droit d’auteur © Valérie Lecœur.  
Toute reproduction ou diffusion sans autorisation est interdite.  
Usage éducatif et démonstratif uniquement.

---

> *Yūrei – L’Esprit du Dragon Perdu*  
> « Même les âmes perdues laissent une trace. »


## Niveau 1 chronométré — 26 octobre 2026

Le lac des lanternes demande désormais de collecter 30 âmes en trois minutes. Le dragon avance continuellement ; les glissements orientent sa trajectoire avec un virage progressif. Toucher le corps coûte 10 secondes et trois segments, puis accorde deux secondes de protection. Les berges font rebondir le dragon sans pénalité. La lumière reflète le temps restant. La pause fige le défi ; la sauvegarde v4 conserve le temps, les collectes, la direction et la protection. Les anciennes parties restent lisibles (nouveau chronomètre pour une ancienne partie en cours, souvenir conservé si elle était terminée). Ces règles remplacent le parcours contemplatif décrit plus haut.

## Niveau 2 — Le jardin des promesses

Après le souvenir du lac, le jardin des sakuras se débloque : cerisiers en fleurs, pétales animés et musique originale. Collectez 30 âmes en trois minutes en évitant la queue et trois pierres bordées de rose. Chaque contact coûte dix secondes et trois segments, puis accorde deux secondes de protection ; les arbres et pétales sont décoratifs. La victoire révèle « La promesse du printemps ». Le niveau en cours est conservé dans la sauvegarde v5, avec compatibilité des anciennes parties. Les deux souvenirs restent débloqués séparément.

Les nouvelles pistes sont reproductibles avec `python tools/generate_sakura_audio.py`.
