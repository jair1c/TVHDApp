# TV HD App

Aplicacion Android para ver canales y eventos desde tvtvhd.com.
Compatible con telefono y Android TV.

## Compilar sin Android Studio (GitHub Actions)

### 1. Subir a GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/TU_USUARIO/TVHDApp.git
git branch -M main
git push -u origin main
```

### 2. Descargar el APK compilado

1. Ve al repo en GitHub
2. Clic en Actions -> ultimo workflow verde
3. Abajo en Artifacts -> descarga TVHDApp-debug-X
4. Extrae el ZIP e instala el .apk

### 3. Release con tag

```bash
git tag v1.0
git push origin v1.0
```
El APK aparece en Releases del repo.

## Actualizar datos

1. Corre python scraper.py en tu PC
2. Copia output/tvhd_data.json a app/src/main/assets/
3. git add . && git commit -m "Update" && git push
