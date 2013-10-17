#!/bin/bash -e

mongo gg_db --eval 'db.addUser("user_name", "user_pass");'
