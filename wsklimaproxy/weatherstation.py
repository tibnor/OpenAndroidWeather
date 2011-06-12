from datetime import datetime, timedelta
from google.appengine.api import urlfetch
from google.appengine.ext import db
from xml.dom import minidom
import time

def getMetData(timeserietypeID, fromTime, toTime, stations, elements, hours, months):
    url = "http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=" \
        + timeserietypeID + "&format=&from=" + fromTime.strftime('%Y-%m-%d')\
        + "&to=" + toTime.strftime('%Y-%m-%d') \
        + "&stations=" + str(stations)\
        + "&elements=" + elements\
        + "&hours=" + hours\
        + "&months=" + months\
        + "&username="
    result = urlfetch.fetch(url);
    return minidom.parseString(result.content)

class WeatherStation(db.Model):
    id = db.IntegerProperty(required=True)
    temperature = db.FloatProperty()
    temperatureUpdated = db.DateTimeProperty()
    temperatureLastTry = db.DateTimeProperty()
    
    def getTempNow(self):
        updatetime = datetime.utcnow() + timedelta(hours= -1);
        if self.temperatureUpdated != None and updatetime < self.temperatureUpdated:
            return  """{"time":%d,"temperature":%s}""" % (time.mktime(self.temperatureUpdated.timetuple()), self.temperature);
        
        # return old data if the data is between 1 hour and 1 hour and 15 minutes. 
        # And last try was within 5 minutes
        minLastTryTime = datetime.utcnow() + timedelta(minutes= -5);
        minDataTime = datetime.utcnow() + timedelta(minutes= -15, hours=-1);
        if self.temperatureLastTry != None and minLastTryTime < self.temperatureLastTry \
            and self.temperatureUpdated != None and minDataTime < self.temperatureUpdated:
            if self.temperatureUpdated != None and self.temperature != None:
                return  """{"time":%d,"temperature":%s}""" % (time.mktime(self.temperatureUpdated.timetuple()), self.temperature);
            else:
                return ""
            
        # return old data if the data is older than 1 hour and 15 minutes. 
        # And last try was within 30 minutes
        maxLastTryTime = datetime.utcnow() + timedelta(minutes= -30);
        maxDataTime = datetime.utcnow() + timedelta(minutes= -15, hours=-1);
        if self.temperatureLastTry != None and maxLastTryTime <= self.temperatureLastTry \
            and self.temperatureUpdated != None and maxDataTime >= self.temperatureUpdated:
            if self.temperatureUpdated != None and self.temperature != None:
                return  """{"time":%d,"temperature":%s}""" % (time.mktime(self.temperatureUpdated.timetuple()), self.temperature);
            else:
                return ""
        
        fromTime = datetime.utcnow() + timedelta(hours= -6);
        toTime = datetime.utcnow();
        hours = '00,01,02,03,04,05,06,07,08,09,10,11,12,13,14,15,16,17,18,19,20,21,22,23';
        months = '1,2,3,4,5,6,7,8,9,10,11,12'
        elements = 'TA'
        dom = getMetData('2', fromTime, toTime, self._id, elements, hours, months)
        items = dom.getElementsByTagName('item');
        fromTimeMax = datetime.fromtimestamp(0)
        temperature = None
        for it in items:
            if it.attributes.values()[0].value == 'ns2:no_met_metdata_TimeStamp':
                fromStr = it.getElementsByTagName('from')[0].firstChild.data;
                fromTime = datetime.strptime(fromStr, "%Y-%m-%dT%H:%M:%S.000Z")
                tempTemperature = it.getElementsByTagName('value')[0].firstChild.data;
                if fromTime > fromTimeMax and tempTemperature != '-99999':
                    fromTimeMax = fromTime
                    temperature = tempTemperature;
        # Save
        self.temperatureLastTry = datetime.utcnow();
        if temperature == None:
            self.temperatureUpdated = datetime.fromtimestamp(0)
            self.put();
            return "";
        self.temperature = float(temperature)
        self.temperatureUpdated = fromTimeMax
        self.put();
        
        return  """{"time":%d,"temperature":%s}""" % (time.mktime(fromTimeMax.timetuple()), temperature);
