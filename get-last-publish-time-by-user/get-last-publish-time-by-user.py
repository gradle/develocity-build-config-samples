#!/usr/bin/env python3

"""
This script demonstrates how to retrieve the last published build time for each user
provided via stdin, using the Develocity REST API to fetch build information.
(https://docs.gradle.com/develocity/api-manual/)

See samples for demos in other languages: https://github.com/gradle/develocity-api-samples

To run it, set environment variables:
  export DEVELOCITY_URL="https://develocity.example.com"
  export DEVELOCITY_ACCESS_KEY="your-access-key"

USAGE:
  cat users.txt | ./get-last-publish-time-by-user.py
"""

import os
import sys
import requests
from datetime import datetime


def get_last_build_info(username):
    """Returns build ID and availableAt time for the most recent build by a user"""
    url = os.environ.get('DEVELOCITY_URL')
    key = os.environ.get('DEVELOCITY_ACCESS_KEY')

    if not url or not key:
        sys.stderr.write("Error: DEVELOCITY_URL and DEVELOCITY_ACCESS_KEY must be set\n")
        sys.exit(1)

    response = requests.get(
      f"{url}/api/builds?maxBuilds=1&reverse=true&query=user:{username}",
      headers={"Authorization": f"Bearer {key}"}
    )
    response.raise_for_status()
    data = response.json()

    if data and len(data) > 0:
        return data[0].get('id'), data[0].get('availableAt')
    return None, None


def print_row(user, time_str, build_id):
    """Format and print a row with user, timestamp and build ID"""
    print(f"{user:<50} | {time_str:<24} | {build_id if build_id else ''}")


def main():
    for line in sys.stdin:
        username = line.strip()
        if not username:
            continue

        build_id, timestamp = get_last_build_info(username)

        if not build_id and timestamp:
            print_row(username, "", "")
            continue

        formatted_time = datetime.fromtimestamp(timestamp).strftime("%Y-%m-%dT%H:%M:%SZ")
        print_row(username, formatted_time, build_id)


if __name__ == "__main__":
    main()
