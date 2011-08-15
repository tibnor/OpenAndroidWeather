from datetime import datetime, timedelta
from google.appengine.api import urlfetch
from google.appengine.ext import db
from xml.dom import minidom
from google.appengine.api import memcache
import logging
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
    temperatureExpires = db.DateTimeProperty()
    timesNotUpdated = db.IntegerProperty(default=0)
    status = int

    
    def getTempNow(self):        
        if self.timesNotUpdated >= 10:
            return self.tempToJson()
        
        now = datetime.utcnow();
        
        if (self.temperatureExpires is not None and self.temperatureExpires > now):
            return self.tempToJson()
        

        
        
        fromTime = datetime.utcnow() + timedelta(hours= -1);
        toTime = datetime.utcnow()
        hours = str(fromTime.hour) + "," + str(toTime.hour)
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
        now = datetime.utcnow()
        fromTime = fromTimeMax
        if (fromTime is None or fromTime < now + timedelta(hours= -2)):
            # If no result or result older than two hours, set expiration to 30 minutes from now
            self.timesNotUpdated += 1 
            self.temperatureExpires = now + timedelta(minutes=30);
        elif (fromTime < now + timedelta(minutes= -50)):
            # If between 50 minute and 2 hours set to 2 minutes 
            self.temperatureExpires = now + timedelta(minutes=2);
        else:
            # If temperature is not older than 50 minutes, set to next hour
            expire = now + timedelta(hours=1);
            expire -= timedelta(minutes=expire.minute, seconds=expire.second)
            self.temperatureExpires = expire
            
            
       
        if temperature is not None:
            self.temperature = float(temperature)
            self.temperatureUpdated = fromTimeMax
        self.put();
        text = self.tempToJson();
        return text;
    
    def saveToMemcach(self, text, status):
        if not memcache.set("tempJSON:" + str(self.id), text, time.mktime(self.temperatureExpires.timetuple())):
            logging.error("memcach not working") 
        if not memcache.set("tempStatus:" + str(self.id), status, time.mktime(self.temperatureExpires.timetuple())):
            logging.error("memcach not working") 
        return    
    
    def tempToJson(self):
        text = None
        if self.temperatureUpdated != None and self.temperature != None and self.timesNotUpdated < 10:
            text = """{"time":%d,"temperature":%s""" % (time.mktime(self.temperatureUpdated.timetuple()), self.temperature);
            text += ""","reliable":true}"""
            self.status = 200
        else:
            text = ""
            self.status = 204
        self.saveToMemcach(text, self.status)
        return text
    
    def getStatus(self):
        return self.status
