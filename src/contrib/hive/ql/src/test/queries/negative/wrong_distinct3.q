FROM SRC
INSERT OVERWRITE TABLE DEST1 SELECT SRC.key, COUNT(DISTINCT SUBSTR(SRC.value,4)) GROUP BY SRC.key
INSERT OVERWRITE TABLE DEST2 SELECT SRC.key, COUNT(DISTINCT SUBSTR(SRC.value,5)) GROUP BY SRC.key
