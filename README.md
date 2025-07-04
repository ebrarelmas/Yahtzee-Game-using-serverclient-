Bu proje, geleneksel Yahtzee zar oyununun iki oyunculu, ağ üzerinden oynanabilen Java 
tabanlı bir masaüstü uygulamasını gerçekleştirmeyi amaçlamaktadır. Oyun, istemci
sunucu mimarisi ile tasarlanmış olup, oyuncuların sırasıyla zar atmasını, puanlarını 
hesaplamasını ve skor tablosuna göre kazananın belirlenmesini sağlar. Uygulama Java 
Swing ile GUI (grafiksel arayüz) içerir ve çok oyunculu destek AWS üzerinden sağlanan bir 
sunucu aracılığıyla gerçekleştirilmiştir. 

YahtzeeClient classındaki String serverAddress = "13.53.42.45"; satırını 
String serverAddress = "localhost"; şeklinde değiştirirseniz aws üzerinden sunucu ip'si almadan kendi bilgisyarınız üzerinden çalıştırabilrisiniz


1-Genel Yapı  
Yahtzee oyunu, iki oyuncunun aynı anda oynayabildiği, TCP/IP protokolü üzerine kurulu bir 
socket tabanlı mimari ile geliştirilmiştir. 

• Sunucu (YahtzeeServer) 
AWS EC2 üzerinde çalışan bir TCP server’dır.  
ServerSocket kullanılarak gelen istemcileri dinler.  
Maksimum 2 oyuncuya izin verir. 
Oyuncular arası veri akışını yönetir. 

• İstemci (YahtzeeClient) 
Oyuncuların GUI aracılığıyla oyuna bağlandığı bir socket istemcisidir.  
Kullanıcının girdiği verileri (ad, hamleler, skor) sunucuya iletir.  
Sunucudan gelen mesajlara göre GUI'yi günceller
