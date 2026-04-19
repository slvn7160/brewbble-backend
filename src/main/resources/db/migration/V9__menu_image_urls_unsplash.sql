-- V9: Switch all menu items to high-quality Unsplash CDN image URLs
--     No local files needed — images served directly from Unsplash CDN

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Classic Milk Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1558618047-3c8c76ca7d13?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Taro Milk Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1563227812-0ea4c22e6cc8?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Brown Sugar Boba';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Matcha Milk Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1546173159-315724a31696?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Mango Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1497534446932-c925b458314e?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Strawberry Lemonade Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1560023907-5f339617ea30?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Passion Fruit Green Tea';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1544145945-f90425340c7e?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Lychee Slush';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Watermelon Mint Slush';

UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=800&h=800&fit=crop&auto=format&q=85'
  WHERE name = 'Peach Oolong Slush';
