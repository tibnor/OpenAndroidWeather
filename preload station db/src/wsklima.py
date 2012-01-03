import urllib
from xml.dom import minidom
import time
import calendar
from gettypes import Station

url = "http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=2&format=&from=2011-06-05&to=2011-06-05&stations=4200&elements=TA&hours=00,01,02,03,04&months=1,2,3,4,5,6,7,8,9,10,11,12&username="
result = urllib.urlopen(url);
dom = minidom.parse(result)
items = dom.getElementsByTagName('item');
fromTimeMax = 0
temperature = None
for it in items:
    if it.attributes.values()[0].value == 'ns2:no_met_metdata_TimeStamp':
        fromStr=it.getElementsByTagName('from')[0].firstChild.data;
        fromTime = time.strptime(fromStr, "%Y-%m-%dT%H:%M:%S.000Z")
        if fromTime > fromTimeMax:
            fromTimeMax = fromTime
            temperature = it.getElementsByTagName('value')[0].firstChild.data;

print("""{time:"%d",temperature:"%s"}""" % (calendar.timegm(fromTimeMax),temperature))
