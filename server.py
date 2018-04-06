from flask import Flask
import json


app = Flask(__name__)
users = {
    'users': [
        {
            'name': 'Fulano Alberto',
            'age': 46
        },
        {
            'name': 'Adriana',
            'age': 6
        },
        {
            'name': 'Ciclano',
            'age': 30
        }
    ]
}


@app.route('/api/get-users')
def hello_world():
    return json.dumps(users)