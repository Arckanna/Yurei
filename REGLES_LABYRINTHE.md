# Yūrei — Règles du jeu en labyrinthe

Version 0.1 · 17 novembre 2026 · Document de conception, à valider par des essais sur téléphone

## 1. Intention

Rapprocher Yūrei de son inspiration d'origine, Meynaupède (Fred & Ted, 1985, Amstrad CPC) : un jeu de serpent en labyrinthe qui demande à la fois des réflexes et de la réflexion. On reprend les mécaniques de ce type de jeu (grille, couloirs, objets à vider, ne pas se mordre). Le nom, les graphismes et les niveaux de Meynaupède ne sont pas repris.

L'identité de Yūrei reste la même : dragon spectral, âmes lumineuses, mondes japonais, souvenirs, ambiance douce, danger lisible et évitable, commande simple au doigt, jeu hors ligne.

Ces règles **remplacent** le défi actuel (terrain ouvert, joystick, 30 âmes en 3 minutes) pour les trois mondes existants.

## 2. Décisions prises (17/11/2026)

| Sujet | Décision |
|---|---|
| Déplacement | Sur une grille, 4 directions, commandé par des glissements du doigt. Le joystick disparaît. |
| Murs | Le dragon s'arrête devant un mur et attend le prochain glissement. Les murs ne punissent jamais. |
| Se mordre la queue | Coûte une lanterne (3 lanternes par tableau). À 0 lanterne, le tableau est perdu. |
| Chronomètre | Simple bonus : finir vite rapporte plus, on ne perd jamais à cause du temps. |
| Découpage | 3 tableaux par monde, de difficulté croissante ; le souvenir arrive après le 3e. |
| Vitesse | Fixe pendant un tableau, un peu plus rapide au tableau suivant. |
| Mondes existants | Lac, jardin et sanctuaire deviennent des labyrinthes, avec leurs décors, musiques et souvenirs. |
| Dangers des mondes 2 et 3 | Conservés et intégrés au labyrinthe (pierres, sceaux). |

## 3. Règles détaillées

Les valeurs chiffrées sont des **points de départ** : elles seront ajustées après essais et chaque ajustement sera noté dans `CHANGELOG.md`.

### 3.1 Le labyrinthe
- La grille occupe le terrain logique de 1080 × 1920 unités. Proposition : cases de 120 unités, soit 9 colonnes × 16 lignes, ce qui laisse de la place pour l'affichage en haut de l'écran.
- Un mur occupe une case entière (choix fait à l'étape 1 : un plan s'écrit alors comme un simple dessin en texte, facile à relire et à corriger). Les labyrinthes sont **dessinés à la main** (pas de génération aléatoire) dans `core/.../maze/MazeCatalog.kt`, avec `#` pour un mur, `.` pour un couloir, `o` pour une âme et `D` pour le départ. On peut les relire, les tester et en ajouter sans modifier le moteur. Le bord de la grille compte comme un mur.
- Un plan n'a **aucun cul-de-sac** (case libre à une seule issue) : le demi-tour étant ignoré, un dragon qui y entrerait avec un corps ne pourrait plus en ressortir. Vérifié par un test pour chaque tableau (ajouté à l'étape 4).
- Le labyrinthe est fermé, sans téléportation d'un bord à l'autre.
- Chaque tableau a une case de départ et une direction de départ.

### 3.2 Les âmes
- Les âmes sont posées à des **emplacements fixes** du plan, toutes visibles dès le début du tableau. Elles ne réapparaissent pas.
- Chaque âme mangée ajoute un segment au corps.
- Le tableau est réussi quand **toutes les âmes** sont recueillies.
- Proposition de départ : environ 15, 20 puis 25 âmes pour les tableaux 1, 2 et 3. Chaque plan est vérifié par un test : toutes les âmes sont atteignables et le tableau peut être fini.

### 3.3 Le déplacement
- Au début du tableau, le dragon attend le premier glissement. Ensuite il avance seul, d'une case à la fois, à vitesse constante.
- Un glissement donne une direction. Elle est **mémorisée** et appliquée dès que possible : à l'arrivée sur la prochaine case si le passage est libre, sinon au premier carrefour qui le permet.
- Le demi-tour direct est ignoré dès que le dragon a un corps, pour éviter une morsure involontaire. Les autres virages sont toujours mémorisés.
- Devant un mur, le dragon s'arrête, tout son corps aussi. Il repart au glissement suivant vers un passage libre.
- Quand le dragon est arrêté, rien ne se passe à son détriment : il ne perd ni lanterne ni temps de bonus.
- Proposition de vitesse : 4 cases par seconde au tableau 1, puis 5 et 6.

### 3.4 Les lanternes (vies)
- Chaque tableau commence avec 3 lanternes.
- Si la tête entre dans une case occupée par le corps, une lanterne s'éteint. Le dragon s'arrête devant le corps et attend un glissement ; il devient translucide pendant 2 secondes de déplacement : il peut alors traverser son propre corps pour se dégager. Entrer dans la case que le bout de la queue est en train de quitter n'est pas une morsure.
- Les âmes déjà recueillies restent acquises.
- À 0 lanterne, le tableau est perdu. On le recommence depuis le début, avec 3 lanternes. Les tableaux déjà réussis et les souvenirs restent acquis.

### 3.5 Score et chronomètre
- Âme recueillie : 10 points.
- Tableau réussi : 500 points.
- Bonus de temps : si le tableau est fini en moins de 90 secondes, chaque seconde restante rapporte 10 points. Au-delà, le bonus vaut 0, sans autre conséquence.
- Le chronomètre ne tourne pas pendant la pause ni pendant l'arrêt après une morsure.
- On garde un **record par monde**. Le score affiché reste discret pour ne pas casser l'ambiance.

### 3.6 Ce qui est affiché pendant la partie
- Lanternes restantes, âmes restantes, temps du bonus, numéro du tableau. Pas de nouveau bouton : la pause reste le seul bouton en jeu.
- Une introduction courte avant chaque monde : le but, le geste (glisser), la nouveauté du monde.

## 4. Les trois mondes

| Monde | Tableaux | Nouveauté |
|---|---|---|
| 1. Le lac des lanternes | 3 | Apprendre le labyrinthe : couloirs larges, peu de carrefours au tableau 1. Seul danger : sa propre queue. |
| 2. Le jardin des promesses | 3 | **Pierres** posées dans certains couloirs, avec une bordure rose bien visible. Contrairement aux murs, la tête qui entre dans une pierre éteint une lanterne. Règle à retenir : *le mur arrête, la pierre blesse*. |
| 3. Le sanctuaire des marées | 3 | **Sceaux** sur certaines cases, qui s'éveillent à tour de rôle : calme, alerte (2 s, double contour doré), actif (2 s, disque vermillon). La tête qui entre dans un sceau actif éteint une lanterne. Un sceau ne s'active jamais sous la tête du dragon sans alerte. |

Les décors (torii, cerisiers, pétales, brume) restent décoratifs. Une âme n'est jamais posée sur une pierre ou un sceau.

## 5. Sauvegarde et progression
- La partie en cours enregistre : monde, tableau, position et direction de chaque segment, direction mémorisée, âmes restantes, lanternes, protection, temps écoulé, score, et l'état des sceaux.
- Cela demande une **sauvegarde v8**. Les souvenirs et les mondes débloqués des anciennes sauvegardes (v1 à v7) sont conservés. Une partie en cours à l'ancien format ne peut pas reprendre dans un labyrinthe : elle est abandonnée avec un message clair, sans toucher à la progression.
- La progression est enregistrée par identifiant de monde et de tableau, plutôt qu'avec un booléen par monde (cf. lot C2 du cahier des charges).

## 6. Ordre de réalisation proposé

Chaque étape se fait sur sa propre branche `yurei-AAAA-MM-JJ-…`, avec ses tests.

1. **Moteur de labyrinthe** (`:core`) : grille et murs lus depuis un plan en texte, déplacement case par case, direction mémorisée, arrêt devant les murs, morsure, lanternes, score. Tests JVM pour chaque règle et pour l'atteignabilité des âmes.
2. **Prototype du lac** : un tableau jouable dans l'interface, glissements à la place du joystick, affichage discret. Essai sur téléphone pour régler vitesse et taille des cases.
3. **Sauvegarde v8** et migration des anciennes sauvegardes.
4. **Les 3 tableaux du lac**, puis ceux du jardin (pierres), puis du sanctuaire (sceaux).
5. **Nettoyage** : retirer l'ancien défi (joystick, `TimedChallenge`, champs hérités), mettre à jour README et CHANGELOG.

L'étape 1 est l'occasion d'introduire le catalogue de niveaux prévu au lot C1 : un tableau = des données (plan, âmes, vitesse, dangers), une mécanique = du code testé.

## 7. Questions ouvertes (à trancher pendant les essais)
- Taille des cases et vitesse confortables sur un petit écran.
- Faut-il un retour par vibration (facultatif) quand le dragon s'arrête ou perd une lanterne ?
- Longueur de départ du dragon (1 segment ou quelques-uns).
- Aide pour les débutants : montrer la direction mémorisée par une petite flèche ?
