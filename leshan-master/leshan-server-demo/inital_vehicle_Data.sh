echo " Setting up initial values for vehicles data  "
sqlite3 IoTParking.db <<EOSs
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA01','Vehicle-04-01','NO','0.0','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA02','Vehicle-04-02','NO','0.0','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA03','Vehicle-04-03','NO','0.0','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA04','Vehicle-04-04','NO','0.0','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA05','Vehicle-04-05','NO','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA06','Vehicle-04-06','NO','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA07','Vehicle-04-07','YES','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA08','Vehicle-04-08','YES','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA09','Vehicle-04-09','YES','0.0','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA10','Vehicle-04-10','YES','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA11','Vehicle-04-11','NO','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA12','Vehicle-04-12','YES','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA13','Vehicle-04-13','NO','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA14','Vehicle-04-14','YES','52.5','');
INSERT INTO REGISTERED_VEHICLES (TIME,VEHID,VEHNAME,CRIMNL_RECD,DUES,COMMENTS) VALUES (10000,'KA15','Vehicle-04-15','YES','52.5','');
EOSs