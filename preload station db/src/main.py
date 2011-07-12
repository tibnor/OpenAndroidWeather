'''
Created on May 29, 2011

@author: torstein
'''
from xml.dom.minidom import parse
import urllib

#conn = sqlite3.connect('stations.db')
#c = conn.cursor();
#
#c.execute('''drop table if exists "android_metadata"''')
#c.execute('''CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US')''');
#c.execute('''INSERT INTO "android_metadata" VALUES ('en_US')''');
#c.execute('''drop table if exists stations''')
#c.execute('''create table if not exists stations (name text, _id integer primary key, lat real, lon real)''')



def lowerName(name):
    name = name.lower()
    name = list(name)
    m = len(name)
    name[0] = name[0].upper()
    
    for i in range(1, m):
        if (((name[i - 1] == " " or name[i - 1] == "-")  and not (name[i] == "i" and name[i + 1] == " ")) or ((name[i - 1] == "I" or name[i -1] == "X") and name[i] == "i")):
            name[i] = name[i].upper()
            
#    i = m-1
#    if (name[i - 1] == " " or (name[i - 1] == "I")):
#        name[i] = name[i].upper()
        
    return "".join(name)

file = urllib.urlopen("http://eklima.met.no/metdata/MetDataService?invoke=getStationsFromTimeserieTypeElemCodes&timeserietypeID=2&elem_codes=TA&username=")
dom = parse(file)
out = open('stations.txt', 'w')

stations = dom.getElementsByTagName('item');
for s in stations:
    toYear = s.getElementsByTagName('toYear')[0].firstChild.data
    if toYear == '0':
        nr = s.getElementsByTagName('stnr')[0].firstChild.data
        name = s.getElementsByTagName('name')[0].firstChild.data
        name = lowerName(name)
        lat = s.getElementsByTagName('latDec')[0].firstChild.data
        long = s.getElementsByTagName('lonDec')[0].firstChild.data
        string = 'insert into stations values (' + nr + ',\'' + name + '\',' + lat + ',' + long + ',1);\n'
        out.write(string)
        #d = c.execute(string)

#conn.commit()
#c.close();
