### Как собрать jimm из исходников:


Тулзы:
- Ant 1.9.16
https://ant.apache.org/bindownload.cgi 
- Proguard 5.2.1
https://sourceforge.net/projects/proguard/files/proguard/5.2/
- Sun Java Wireless Toolkit 2.5.2_01
https://www.oracle.com/java/technologies/java-archive-downloads-javame-downloads.html#sun_java_wireless_toolkit-2.5.2_01b-oth-JPR
- JDK 1.6.13 (x86) 
https://www.oracle.com/ru/java/technologies/javase-java-archive-javase6-downloads.html

Дополнительно, но не обязательно:
- NetBeans 8.2 with JDK 8u111
https://archive.org/details/jdk-8u111-nb-8_2
- Git (для win 2k и )
https://git-scm.com/downloads

Последняя версия git для win 2k и xp:
https://github.com/git-for-windows/git/releases/tag/v2.10.0.windows.1

Ant и JDK(6) должны быть добавлены в переменные среды, или прописаны в локальной сессии до папки bin

Открываем скрипт build.xml, указываем пути до Java ME SDK и Proguard.
Сборка командой "ant", очистка сборки "ant clean"

Баги: не работает сборка под midp1.0 и нельзя собрать сборку без смайликов
