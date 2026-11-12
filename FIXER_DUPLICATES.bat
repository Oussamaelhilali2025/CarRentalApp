@echo off
echo ========================================
echo  Suppression des fichiers XML en double
echo ========================================

set RES=app\src\main\res

del /f /q "%RES%\mipmap-hdpi\ic_launcher.xml"       2>nul
del /f /q "%RES%\mipmap-hdpi\ic_launcher_round.xml" 2>nul
del /f /q "%RES%\mipmap-mdpi\ic_launcher.xml"       2>nul
del /f /q "%RES%\mipmap-mdpi\ic_launcher_round.xml" 2>nul
del /f /q "%RES%\mipmap-xhdpi\ic_launcher.xml"      2>nul
del /f /q "%RES%\mipmap-xhdpi\ic_launcher_round.xml" 2>nul
del /f /q "%RES%\mipmap-xxhdpi\ic_launcher.xml"     2>nul
del /f /q "%RES%\mipmap-xxhdpi\ic_launcher_round.xml" 2>nul
del /f /q "%RES%\mipmap-xxxhdpi\ic_launcher.xml"    2>nul
del /f /q "%RES%\mipmap-xxxhdpi\ic_launcher_round.xml" 2>nul

echo.
echo Fichiers XML suppresses avec succes !
echo Vous pouvez maintenant relancer Build dans Android Studio.
echo.
pause
