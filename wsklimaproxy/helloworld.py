from google.appengine.api import memcache
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from weatherstation import WeatherStation




class MainPage(webapp.RequestHandler):
    
    def get(self):
        stationId = int(self.request.get("st"))
        status = None
        text = memcache.get("tempJSON:"+str(id))
        if text is None:
            station = WeatherStation.get_or_insert(str(stationId), id=stationId)
            text = station.getTempNow()
            status = station.getStatus()
        
        self.response.set_status(status)
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.out.write(text)
        
class BlackList(webapp.RequestHandler):
    def get(self):
        query = WeatherStation.all()
        query.filter('timesNotUpdated >=', 10)
        text = "{\"station\":["
        if query.count(1)>0:
            for station in query:
                text += str(station.id) + ","
            text = text.replace(' ', '')[:-1]
        
        
        text += "]}"   
        
        self.response.out.write(text)


application = webapp.WSGIApplication([('/temperature', MainPage),('/blacklist',BlackList)], debug=True)


def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()
    


        


    
        
        
