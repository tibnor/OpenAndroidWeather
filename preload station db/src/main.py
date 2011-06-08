'''
Created on May 29, 2011

@author: torstein
'''
import sqlite3
import urllib
from xml.dom.minidom import parse

#conn = sqlite3.connect('stations.db')
#c = conn.cursor();
#
#c.execute('''drop table if exists "android_metadata"''')
#c.execute('''CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US')''');
#c.execute('''INSERT INTO "android_metadata" VALUES ('en_US')''');
#c.execute('''drop table if exists stations''')
#c.execute('''create table if not exists stations (name text, _id integer primary key, lat real, lon real)''')

file = urllib.urlopen("http://eklima.met.no/metdata/MetDataService?invoke=getStationsFromTimeserieTypeElemCodes&timeserietypeID=2&elem_codes=TA&username=")
dom = parse(file)
out = open('stations.txt','w')

stations = dom.getElementsByTagName('item');
for s in stations:
    toYear = s.getElementsByTagName('toYear')[0].firstChild.data
    # change to toYear = '0'
    if toYear == '2011':
        nr = s.getElementsByTagName('stnr')[0].firstChild.data
        name = s.getElementsByTagName('name')[0].firstChild.data
        lat = s.getElementsByTagName('latDec')[0].firstChild.data
        long = s.getElementsByTagName('lonDec')[0].firstChild.data
        string = 'insert into stations values ('+nr+',\''+name+'\','+lat+','+long+');\n'
        out.write(string)
        #d = c.execute(string)

#conn.commit()
#c.close();