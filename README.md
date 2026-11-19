# Regarde ici ! — prototype Android

Caméra arrière sur l’écran du parent, personnage animé ou vidéo locale sur un second écran autorisé par Android. Interface en français. Cible matérielle : Samsung Galaxy Z Flip6, à valider sur appareil réel.

## Télécharger et installer

**[Télécharger l’APK Android](https://github.com/ablanchard/regarde-ici/releases/download/v0.1.0/RegardeIci-prototype.apk)** · [Page de téléchargement](https://github.com/ablanchard/regarde-ici/releases/tag/v0.1.0)

Depuis le téléphone, télécharger l’APK, l’ouvrir avec **Mes fichiers**, puis suivre les instructions d’installation et autoriser la caméra au premier lancement. Si Android le demande, autoriser l’installation depuis cette source.

Cette préversion est signée pour le développement. Les photos et animations ont été testées sur émulateur ; l’accès simultané aux deux écrans du Flip6 reste à vérifier sur le téléphone.

Voir aussi le [guide d’installation](INSTALLATION.txt). Si l’écran externe reste noir, ouvrir **Aide → Diagnostic → Copier** dans l’application pour obtenir les informations de compatibilité.

## Limite essentielle

Ce prototype **ne garantit pas le fonctionnement simultané des deux écrans du Flip6**. Une application ordinaire ne peut pas forcer l’allumage de son écran externe. `Presentation` fonctionne uniquement quand Android autorise l’accès au second écran. Les versions Android antérieures à Android 16 ne prennent pas en charge les présentations sur les écrans internes intégrés ; les versions suivantes restent soumises aux règles du système et du fabricant. Le code tente l’affichage sur l’écran choisi, intercepte les refus, observe les changements d’écran et fournit un diagnostic. Une fenêtre acceptée n’est pas présentée comme une preuve de visibilité physique. Aucun contournement Samsung, Shizuku, root ou API privée n’est inclus.

Good Lock / MultiStar permet certains usages de l’écran externe, mais ne garantit pas le fonctionnement simultané nécessaire ici. Ne pas considérer une démonstration sur écran simulé comme une validation Samsung.

Référence : https://developer.android.com/reference/android/app/Presentation

## Fonctions

- CameraX : caméra arrière par défaut, aperçu, mise au point tactile, zoom 1× / 2× (limité aux capacités disponibles).
- JPEG dans `Pictures/RegardeIci` via MediaStore, sans autorisation générale de stockage. La résolution est négociée par CameraX ; le mode Samsung 50 MP n’est pas reproduit.
- Déclencheur tactile ou touches de volume, retardateur de 3 secondes, flash désactivé ou automatique.
- Chat, canard et lapin originaux dessinés sur Canvas, animations douces, position centre / gauche / droite.
- Import par le sélecteur de fichiers Android, vidéo locale lisible par VideoView, muette et en boucle sur le second écran. Si le format ne peut pas être décodé, retour au personnage.
- L’aperçu « Voir le personnage ici » permet de voir le personnage sur l’écran principal ; il ne valide pas l’écran externe.
- Arrêt de l’animation et de l’usage caméra lorsque l’application quitte le premier plan. Annulation du retardateur en arrière-plan. Aucun service caméra en arrière-plan.
- Diagnostic copiable sans photos : modèle, Android, identifiant logiciel, écrans exposés, état, flags et résultat du dernier essai.
- Aucun accès Internet, publicité, compte ou envoi de photos.

## Compiler

JDK 17, Android SDK Platform 35 et accès aux dépôts Google/Maven pour télécharger les dépendances. Configurer `ANDROID_HOME` ou un `local.properties` contenant `sdk.dir=/chemin/vers/Android/sdk`.

```sh
./gradlew assembleDebug lintDebug
```

Résultat : `app/build/outputs/apk/debug/app-debug.apk`. Versions fixées : Gradle 8.9, Android Gradle Plugin 8.7.3, CameraX 1.5.3, AndroidX Activity 1.10.1. Android 11 minimum, cible Android 15. Le Flip6 a été lancé avec une version Android supérieure à ce minimum.

L’APK fourni est signé pour le développement. Une distribution en production nécessiterait une signature de production, une validation sur plusieurs versions One UI et une stratégie d’accès à l’écran externe validée matériellement.

## Essai sur le Flip6

1. Installer l’APK sur le téléphone et autoriser la caméra.
2. Garder le téléphone ouvert, les objectifs arrière vers l’enfant.
3. Choisir le personnage ou importer une vidéo personnelle.
4. Appuyer sur « Activer l’écran externe », puis choisir l’écran correspondant si proposé.
5. Vérifier de l’autre côté du téléphone que l’animation est réellement visible. Si l’écran reste noir ou si l’accès est refusé, ouvrir Aide → Diagnostic et copier le texte.
6. Si l’animation est visible, prendre une photo et vérifier sa présence dans la galerie, dossier RegardeIci.

Une validation complète exige un Flip6 : visibilité simultanée réelle, veille, pliage, rotation, changement d’application, photo pendant l’animation et redémarrage. Ne pas déclarer la compatibilité matérielle sur la seule base de la compilation ou de l’émulateur.
