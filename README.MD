# Kueep recommendation system

#### WHAT THIS CODE DOES?
this is java code which uses google analytics in oreder to learn uses prefernces (visited pages, time on page, etc) and build a recommendation to each user of prefered product.

#### HOW IT WORKS?
*we are using a web app (in order to run in heroku) with tomcat container.
*connect to our DB in Parse, pull all users prefrences, product & groups.
*pull all the data from google analytics for the past day.
*run algorithm to calculate the score of each product for each user.
*update the DB with updated recommendations.

#### HOW TO RUN?
in order to run you will need parse_application_id, parse_rest_api_key, .p12 file from google project.
to run just hit run on Main class.