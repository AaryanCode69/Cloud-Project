#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://20.219.211.4}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"

if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "Set ACCESS_TOKEN to a valid Entra access token before running."
  exit 1
fi

URL="${BASE_URL%/}/product/api/products"

products=(
'{"name":"Bluetooth Headphones","description":"Noise-cancelling over-ear headphones","price":2499.50,"category":"Electronics"}'
'{"name":"Notebook","description":"200-page ruled notebook","price":120.00,"category":"Stationery"}'
'{"name":"Water Bottle","description":"1L stainless steel insulated bottle","price":899.00,"category":"Home"}'
'{"name":"Gaming Keyboard","description":"Mechanical RGB keyboard","price":3499.99,"category":"Electronics"}'
'{"name":"Backpack","description":"Laptop backpack with multiple compartments","price":1599.00,"category":"Accessories"}'
'{"name":"Running Shoes","description":"Lightweight sports running shoes","price":2999.00,"category":"Footwear"}'
'{"name":"Coffee Mug","description":"Ceramic mug with 350ml capacity","price":199.99,"category":"Kitchen"}'
'{"name":"Desk Lamp","description":"LED desk lamp with adjustable brightness","price":799.00,"category":"Home"}'
'{"name":"Smart Watch","description":"Fitness tracker with heart rate monitor","price":4999.00,"category":"Electronics"}'
'{"name":"Pen Set","description":"Pack of 10 smooth ballpoint pens","price":150.00,"category":"Stationery"}'
'{"name":"Yoga Mat","description":"Non-slip exercise yoga mat","price":899.00,"category":"Fitness"}'
'{"name":"Sunglasses","description":"UV protection stylish sunglasses","price":1299.00,"category":"Accessories"}'
'{"name":"Portable Charger","description":"10000mAh power bank","price":1199.00,"category":"Electronics"}'
'{"name":"Office Chair","description":"Ergonomic chair with lumbar support","price":6999.00,"category":"Furniture"}'
'{"name":"Table Fan","description":"3-speed portable table fan","price":1399.00,"category":"Home Appliances"}'
'{"name":"Hard Drive","description":"1TB external hard disk","price":4499.00,"category":"Electronics"}'
'{"name":"T-Shirt","description":"Cotton casual wear t-shirt","price":499.00,"category":"Clothing"}'
'{"name":"Electric Kettle","description":"1.5L fast boiling kettle","price":1299.00,"category":"Kitchen"}'
'{"name":"Study Table","description":"Wooden study table with storage","price":5499.00,"category":"Furniture"}'
)

for product in "${products[@]}"
do
  echo "Sending: $product"

  curl --fail --silent --show-error --request POST "$URL" \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer $ACCESS_TOKEN" \
    --data "$product"

  printf '\n----------------------\n\n'
done
