NOTE:

A good reference on signing jars is available at:

http://www.dallaway.com/acad/webstart/

To use the thawte cleaner:
jar -jar thawtecleaner.jar my.cert

a 'my.cert.clean' will be created, which can be imported by keytool.

Thawte Cleaner was created by Nicolas Carranzas


overview from the above website:
-------------------------------
keytool -genkey -keyalg RSA -keystore keystore -alias dallaway
keytool -certreq -keystore keystore -file csr.txt -alias dallaway
jar -jar thawtecleaner.jar my.cert and it will create my.cert.clean
keytool -import -file my.cert.clean -alias dallaway -trustcacerts -keystore keystore
