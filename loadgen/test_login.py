import datetime
import random
import json
import requests
import subprocess

from common import get_ip_map, UserBase, get_random_time

class User(UserBase):
    def __init__(self, username, password, ip_map, port_map):
        super().__init__(username, password, ip_map, port_map)

    def get_addr(self, service, path):
        return 'http://' + self.ip_map[service] + ':' + self.port_map[service] + path

    def auth_headers(self):
        return {'Authorization': 'Bearer ' + self.token}

    def login(self):
        response = requests.post(self.get_addr('ui-dashboard', "/api/v1/users/login"),
                                 json={'username':self.username, 'password': self.password, 'verificationCode':'1234'})
        data = json.loads(response._content)['data']
        self.token = data['token']
        self.userId = data['userId']

    def getAllTrips(self):
        self.login()
        response = requests.get(self.get_addr('travel-service', "/api/v1/travelservice/trips"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def getAllStations(self):
        response = requests.get(self.get_addr('station-service', "/api/v1/stationservice/stations"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def getAllContacts(self):
        response = requests.get(self.get_addr('contacts-service', "/api/v1/contactservice/contacts"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def getAllRoutes(self):
        response = requests.get(self.get_addr('route-service', "/api/v1/routeservice/routes"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def choose_two(self, l):
        return random.sample(l, 2)

    def test(self):
        self.login()
        response = requests.get(self.get_addr('order-service', "/api/v1/orderservice/order"),
                                headers=self.auth_headers())
        print(response._content)

    def query_orders(self):
        choice = random.choice([0, 1, 2])
        if choice == 0:
            start_travel_date = get_random_time(100)
            end_travel_date = start_travel_date + 84000*random.randint(10, 100)*1000
            response = requests.post(self.get_addr('order-service', "/api/v1/orderservice/order/query"),
                                     json = {
                                         "loginId": self.userId,
                                         "enableTravelDateQuery": True,
                                         "boughtDateStart": start_travel_date,
                                         "travelDateEnd": end_travel_date
                                     }, headers=self.auth_headers())
        elif choice == 1:
            response = requests.post(self.get_addr('order-service', "/api/v1/orderservice/order/query"),
                                     json = {
                                         "loginId": self.userId,
                                         "enableStateQuery": True,
                                         "state": random.choice([0, 1])
                                     }, headers=self.auth_headers())
        else:
            response = requests.post(self.get_addr('order-service', "/api/v1/orderservice/order/query"),
                                     json = {
                                         "loginId": self.userId,
                                         "enableBoughtDateQuery": True,
                                         "boughtDateStart": start_travel_date,
                                         "boughtDateEnd": end_travel_date
                                     }, headers=self.auth_headers())
        print(response, response._content)

    def getAllOrders(self):
       response = requests.get(self.get_addr('order-service', "/api/v1/orderservice/order"),
                                headers=self.auth_headers())
       return json.loads(response._content)['data']

    def get_sold_tickets(self):
        random_order = random.choice(self.getAllOrders())
        travel_date = random_order['travelDate']
        train_number = random_order['trainNumber']
        response = requests.post(self.get_addr('order-service', "/api/v1/orderservice/order/tickets"),
                                 json = {
                                     "travelDate": travel_date,
                                     "trainNumber": train_number
                                 }, headers=self.auth_headers())
        print(response, response._content)

    def query_already_sold_orders(self):
        random_order = random.choice(self.getAllOrders())
        travel_date = random_order['travelDate']
        train_number = random_order['trainNumber']
        response = requests.post(self.get_addr('order-service', "/api/v1/orderservice/order/query_already_sold_orders"),
                                 json = {
                                     "travelDate": travel_date,
                                     "trainNumber": train_number
                                 }, headers=self.auth_headers())
        print(response, response._content)

    def get_route_from_trip(self, trip):
       response = requests.get(self.get_addr('route-service', "/api/v1/routeservice/routes/" + trip['routeId']),
                               headers=self.auth_headers())
       return json.loads(response._content)['data']

    def get_left_ticket_of_interval(self):
        # random_order = random.choice(self.getAllOrders())
        # travel_date = random_order['travelDate']
        travel_date = get_random_time(100)
        trip = random.choice(self.getAllTrips())
        train_number = trip['tripId']['type'] + trip['tripId']['number']
        route = self.get_route_from_trip(trip)
        ids = random.sample(range(0, len(route['stations'])), 2)
        ids.sort()
        start_station = route['stations'][ids[0]]
        dest_station = route['stations'][ids[1]]
        print(trip, route, start_station, dest_station)
        response = requests.post(self.get_addr('seat-service', "/api/v1/seatservice/seats/left_tickets"),
                                 json = {
                                     "travelDate": travel_date,
                                     "trainNumber": train_number,
                                     "startStation": start_station,
                                     "destStation": dest_station,
                                     "seatType": random.choice([1, 2, 3, 4, 5, 6, 7, 8])
                                 }, headers=self.auth_headers())
        print(response, response._content)

    def get_food_for_trip(self, trip_date_time, start, end, tripId):
        response = requests.get(self.get_addr('food-service', "/api/v1/foodservice/foods/%s/%s/%s/%s" %
                                              (trip_date_time, start, end, tripId)),
                                headers=self.auth_headers())
        food = json.loads(response._content)['data']
        print(food)
        return food

    def get_a_trip_between_stations(self, start, end, trip_date):
        ret_trip = {}
        if random.choice([0, 1]) == 0:
            response = requests.post(self.get_addr('route-plan-service', "/api/v1/routeplanservice/routePlan/" +
                                                   random.choice(['cheapestRoute', 'quickestRoute', 'minStopStations'])),
                                     json = {
                                         "formStationName": start,
                                         "toStationName": end,
                                         "travelDate": trip_date
                                     },
                                     headers=self.auth_headers())
            trips = json.loads(response._content)['data']
            if len(trips) == 0: return None
            trip = random.choice(trips)
            ret_trip['tripId'] = trip['tripId']
            ret_trip['startingStation'] = trip['fromStationName']
            ret_trip['terminalStation'] = trip['toStationName']
        else:
            response = requests.post(self.get_addr('travel-service', "/api/v1/travelservice/trips/left"),
                                     json = {
                                         "startingPlace": start,
                                         "endPlace": end,
                                         "departureTime": trip_date
                                     },
                                     headers=self.auth_headers())
            trips = json.loads(response._content)['data']
            if len(trips) == 0: return None
            trip = random.choice(trips)
            ret_trip['tripId'] = trip['tripId']['type'] + trip['tripId']['number']
            ret_trip['startingStation'] = trip['startingStation']
            ret_trip['terminalStation'] = trip['terminalStation']
        return ret_trip

    def place_random_order(self):
        # Get all routes
        routes = self.getAllRoutes()
        route = random.sample(routes, 1)[0]['stations']
        route_ids = random.sample(range(0, len(route)), 2)
        route_ids.sort()
        start, end = route[route_ids[0]], route[route_ids[1]]
        trip_date_time = get_random_time(100)

        trip = self.get_a_trip_between_stations(start, end, trip_date_time)
        if trip == None:
            print('Returning')
            return
        tripId = trip['tripId']
        contact_id = random.choice(self.getAllContacts())['id']

        food_list = self.get_food_for_trip(trip_date_time, trip['startingStation'], trip['terminalStation'], tripId)
        food = None
        if food_list is not None:
            if random.choice([0, 1]) == 0:
                if food_list and food_list['trainFoodList'] and food_list['trainFoodList'][0]:
                    food = random.choice(food_list['trainFoodList'][0]['foodList'])
            else:
                fsmap = food_list['foodStoreListMap']
                if fsmap is not None:
                    food = random.choice(random.choice(random.choice(list(fsmap.values())))['foodList'])

        print(food)

        order_request_data = {
            "accountId": self.userId,
            "contactsId": contact_id,
            "tripId": tripId,
            "seatType": random.choice([1, 2, 3, 4, 5, 6, 7, 8]),
            "date": trip_date_time,
            "from": trip['startingStation'],
            "to": trip['terminalStation'],
            "assurance": "",
            "foodType": "",
            "stationName": trip['startingStation'],
            "storeName": "",
            "foodName": food['foodName'] if food else "",
            "foodPrice": food['price'] if food else "",
            "handleDate": "",
            "consigneeName": "",
            "consigneePhone": "",
            "consigneeWeight": "",
            "isWithin": "",
        }

        response = requests.post(self.get_addr('preserve-service', "/api/v1/preserveservice/preserve"),
                                 json = order_request_data, headers=self.auth_headers())
        print("MUMMA: ", response, response._content)
        print(self.userId, trip['startingStation'], trip['terminalStation'])
        print("MIA: ", self.getAllOrders())

    def get_food(self):
        response = requests.get(self.get_addr('food-map-service', "/api/v1/foodmapservice/trainfoods"),
                                headers=self.auth_headers())
        print("MAMMA: ", response, response._content)
        response = requests.get(self.get_addr('food-map-service', "/api/v1/foodmapservice/foodstores"),
                                headers=self.auth_headers())
        print("MIA: ", response, response._content)


    def run(self):
        self.login()
        self.place_random_order()
        # self.get_est_route()

ip_map, port_map = get_ip_map("3a4205d9a390")

user = User('fdse_microservice', '111111', ip_map, port_map)
user.run()
# user.test()
