import requests
import json
import datetime
import time
import sys

'''
dt1 =raw_input('Enter Start Time: ')
dt2 = raw_input('Enter Number of hours: ')


StartTime = int(time.mktime(datetime.datetime.strptime(dt1,"%d/%m/%Y %H:%M:%S").timetuple()))

End = int(time.mktime(datetime.datetime.strptime(dt2,"%d/%m/%Y %H:%M:%S").timetuple()))
'''
StartTime=int(time.time())+5
End = StartTime+30
VehiclePlateNumber = "VEH_1"
query = "http://192.168.178.44:8080/api/clients/httpQuery/"+str(StartTime)+"/"+str(End)+"/"+str(VehiclePlateNumber)
print StartTime
r = requests.put(query)

print r
print r.content
response=r.content.split(";")
print response
'''
print response[1]

if response[0]=='0':
	print "Retry"
	sys.exit(0)
print "Records :"+response[1]
'''
availableSpots=response[0].split(",")


print "SpotID"+"       "+"SpotName"
if availableSpots[0] == '':
	print "No spots are availabel for the requested time"
else:
	for i in range(len(availableSpots)):
		if availableSpots[i] != 'null':
			print str(i)+"    "+availableSpots[i]
#sys.exit("user end")

#toPi = requests.put("http://192.168.178.39:8080/api/clients/httpQuery/1547129257/1547130257/Veh_1/",json={'id':32802,'value':'test123'})
#toPi = requests.put("http://192.168.178.44:8080/api/clients/choice/123/321/LeshanClientDemo/car123",json={'id':32802,'value':'test123'})

#INSERT inTO REGISTERED_VEHICLES (VEHID,LICPLNUM) values ('VEH_1','5678');


#response = resp[1:-1]
#js = json.loads(response)


Choice =input('Enter SpotID to reserve: ')
#Carnumber =raw_input('Enter Car LicensePlate number: ')

query = "http://192.168.178.44:8080/api/clients/choice/"+str(StartTime)+"/"+str(End)+"/"+str(availableSpots[Choice])+"/"+str(Vehicle_ID)

r = requests.put(query)
print "done"


resp= r.content
print resp


#CREATE TABLE RESERVATION (PIID TEXT PRIMARy KEY , H1 TEXT, H2 TEXT, H3 TEXT,H4 TEXT, H5 TEXT, H6 TEXT, H7 TEXT, H8 TEXT, H9 TEXT,H10 TEXT, H11 TEXT, H12 TEXT, H13 TEXT, H14 TEXT, H15 TEXT,H16 TEXT, H17 TEXT, H18 TEXT, H19 TEXT, H20 TEXT, H21 TEXT,H22 TEXT, H23 TEXT, H24 TEXT);

'''
CREATE TABLE rep_reuniao2 (ID INT, ROOM INT, DATE_BEGIN INT, DATE_END INT);


INSERT into LeshanClientDemo (TIME,RSTART,REND) values (100001,4,5);

SELECT rep_reuniao2.ID, CASE WHEN EXISTS ( 

SELECT * 
FROM rep_reuniao2 r
WHERE (12 BETWEEN r.DATE_BEGIN AND r.DATE_END)
    OR (13 BETWEEN r.DATE_BEGIN AND r.DATE_END)
	    OR (12 <= r.DATE_BEGIN AND 13 >= r.DATE_END)

	) THEN 'FALSE' ELSE 'TRUE' ;


SELECT  (case when count(*) = 0 then 'false' else 'true' end) as HasOverlappingRooms
FROM LeshanClientDemo r
WHERE (2 BETWEEN r.RSTART AND r.REND)
    OR (5 BETWEEN r.RSTART AND r.REND)
	    OR (2 <= r.RSTART AND 5 >= r.REND);'''
