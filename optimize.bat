@echo off
setlocal

set classpath=%classpath%;libs/7zip-4.65.jar
set classpath=%classpath%;libs/AutoComplete-1.4.1.jar
set classpath=%classpath%;libs/DDS2-Charts-5.36.jar
set classpath=%classpath%;libs/dds2-common-2.3.54.jar
set classpath=%classpath%;libs/DDS2-Connector-1.1.36.jar
set classpath=%classpath%;libs/DDS2-jClient-JForex-2.14.4.jar
set classpath=%classpath%;libs/files
set classpath=%classpath%;libs/greed-common-162.jar
set classpath=%classpath%;libs/indicators-sources.jar
set classpath=%classpath%;libs/JForex-API-2.6.43.jar
set classpath=%classpath%;libs/log4j-1.2.14.jar
set classpath=%classpath%;libs/mina-core-1.1.7.jar
set classpath=%classpath%;libs/mina-filter-ssl-1.1.7.jar
set classpath=%classpath%;libs/RstaSpellChecker-1.4.1.jar
set classpath=%classpath%;libs/slf4j-api-1.5.8.jar
set classpath=%classpath%;libs/slf4j-log4j12-1.5.8.jar
set classpath=%classpath%;libs/ta-lib-0.4.4dc.jar
set classpath=%classpath%;libs/transport-client-2.3.54.jar
set classpath=%classpath%;libs/smtp.jar
set classpath=%classpath%;libs/mailapi.jar
set classpath=%classpath%;libs/activation.jar
set classpath=%classpath%;bin/
set classpath=%classpath%;config/

java cz.tradingods.optimizer.Main
