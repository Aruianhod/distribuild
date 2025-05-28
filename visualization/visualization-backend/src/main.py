import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))  # DON'T CHANGE THIS !!!

from flask import Flask, jsonify, request
from flask_socketio import SocketIO
from flask_cors import CORS
import json
import random
import time
import threading
import datetime

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})
socketio = SocketIO(app, cors_allowed_origins="*")

# 模拟数据
user_behaviors = {
    'view': 0,
    'click': 0,
    'cart': 0,
    'purchase': 0
}

recommendation_categories = {
    '电子产品': 0,
    '服装': 0,
    '食品': 0,
    '家居': 0,
    '图书': 0
}

system_metrics = {
    'kafka_throughput': [],
    'flink_throughput': [],
    'end_to_end_latency': [],
    'cpu_usage': [],
    'memory_usage': []
}

# 初始化一些历史数据
for i in range(30):
    timestamp = time.time() - (30 - i) * 60  # 过去30分钟的数据
    system_metrics['kafka_throughput'].append({
        'timestamp': timestamp,
        'value': random.randint(5000, 15000)
    })
    system_metrics['flink_throughput'].append({
        'timestamp': timestamp,
        'value': random.randint(2000, 10000)
    })
    system_metrics['end_to_end_latency'].append({
        'timestamp': timestamp,
        'value': random.randint(150, 350)
    })
    system_metrics['cpu_usage'].append({
        'timestamp': timestamp,
        'value': random.randint(20, 80)
    })
    system_metrics['memory_usage'].append({
        'timestamp': timestamp,
        'value': random.randint(30, 90)
    })

# 模拟热门商品
hot_items = [
    {'id': 'item1', 'name': 'iPhone 13', 'count': random.randint(50, 200)},
    {'id': 'item2', 'name': '华为 Mate 40 Pro', 'count': random.randint(50, 200)},
    {'id': 'item3', 'name': '小米 12', 'count': random.randint(50, 200)},
    {'id': 'item4', 'name': 'MacBook Pro', 'count': random.randint(50, 200)},
    {'id': 'item5', 'name': '联想 ThinkPad', 'count': random.randint(50, 200)},
    {'id': 'item6', 'name': 'Nike 运动鞋', 'count': random.randint(50, 200)},
    {'id': 'item7', 'name': 'Adidas 运动裤', 'count': random.randint(50, 200)},
    {'id': 'item8', 'name': '优衣库 T恤', 'count': random.randint(50, 200)},
    {'id': 'item9', 'name': 'H&M 连衣裙', 'count': random.randint(50, 200)},
    {'id': 'item10', 'name': 'Zara 外套', 'count': random.randint(50, 200)}
]

# 模拟用户活跃度
user_activity = {}
for hour in range(24):
    user_activity[hour] = {}
    for day in range(7):
        user_activity[hour][day] = random.randint(10, 100)

# 模拟推荐理由
recommendation_reasons = [
    {'text': '因为您喜欢智能手机', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢华为', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢小米', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢苹果', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢运动装备', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢时尚服装', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢休闲装', 'weight': random.randint(5, 20)},
    {'text': '因为您喜欢零食', 'weight': random.randint(5, 20)},
    {'text': '热门商品推荐', 'weight': random.randint(5, 20)},
    {'text': '新品推荐', 'weight': random.randint(5, 20)},
    {'text': '购买了相关商品', 'weight': random.randint(5, 20)},
    {'text': '浏览了相关商品', 'weight': random.randint(5, 20)},
    {'text': '与您的兴趣相符', 'weight': random.randint(5, 20)},
    {'text': '其他用户也喜欢', 'weight': random.randint(5, 20)},
    {'text': '限时促销', 'weight': random.randint(5, 20)}
]

# 模拟推荐转化率
conversion_rates = []
for i in range(30):
    date = datetime.datetime.now() - datetime.timedelta(days=29-i)
    conversion_rates.append({
        'date': date.strftime('%Y-%m-%d'),
        'clickRate': random.uniform(0.1, 0.5),
        'cartRate': random.uniform(0.05, 0.3),
        'purchaseRate': random.uniform(0.01, 0.1)
    })

# 模拟个人推荐历史
personal_recommendations = []
for i in range(20):
    timestamp = time.time() - random.randint(1, 86400 * 7)  # 过去一周内的随机时间
    items = []
    for j in range(random.randint(3, 5)):
        item_index = random.randint(0, 9)
        items.append({
            'itemId': f'item{item_index+1}',
            'name': hot_items[item_index]['name'],
            'score': round(random.uniform(0.6, 0.95), 2),
            'reason': recommendation_reasons[random.randint(0, len(recommendation_reasons)-1)]['text']
        })
    
    personal_recommendations.append({
        'userId': 'user1',
        'timestamp': timestamp,
        'items': items
    })

# 按时间排序
personal_recommendations.sort(key=lambda x: x['timestamp'])

# 模拟实时数据生成
def generate_realtime_data():
    while True:
        # 更新用户行为数据
        behavior_type = random.choice(['view', 'click', 'cart', 'purchase'])
        user_behaviors[behavior_type] += 1
        
        # 更新推荐类别数据
        category = random.choice(list(recommendation_categories.keys()))
        recommendation_categories[category] += 1
        
        # 更新系统指标数据
        current_time = time.time()
        
        # 保持最近30分钟的数据
        for metric in system_metrics:
            # 移除旧数据
            system_metrics[metric] = [item for item in system_metrics[metric] if current_time - item['timestamp'] < 1800]
            
            # 添加新数据
            if metric == 'kafka_throughput':
                value = random.randint(5000, 15000)
            elif metric == 'flink_throughput':
                value = random.randint(2000, 10000)
            elif metric == 'end_to_end_latency':
                value = random.randint(150, 350)
            elif metric == 'cpu_usage':
                value = random.randint(20, 80)
            elif metric == 'memory_usage':
                value = random.randint(30, 90)
            
            system_metrics[metric].append({
                'timestamp': current_time,
                'value': value
            })
        
        # 发送实时数据到客户端
        socketio.emit('user_behavior', json.dumps({
            'type': behavior_type,
            'timestamp': current_time,
            'counts': user_behaviors
        }))
        
        socketio.emit('recommendation', json.dumps({
            'category': category,
            'timestamp': current_time,
            'counts': recommendation_categories
        }))
        
        socketio.emit('system_metrics', json.dumps({
            'kafka_throughput': system_metrics['kafka_throughput'][-1],
            'flink_throughput': system_metrics['flink_throughput'][-1],
            'end_to_end_latency': system_metrics['end_to_end_latency'][-1],
            'cpu_usage': system_metrics['cpu_usage'][-1],
            'memory_usage': system_metrics['memory_usage'][-1]
        }))
        
        # 随机更新热门商品排名
        for item in hot_items:
            item['count'] += random.randint(-5, 10)
            if item['count'] < 0:
                item['count'] = 0
        
        hot_items.sort(key=lambda x: x['count'], reverse=True)
        
        # 休眠一段时间
        time.sleep(3)

# API路由
@app.route('/api/dashboard/kpi', methods=['GET'])
def get_dashboard_kpi():
    return jsonify({
        'activeUsers': random.randint(100, 500),
        'totalRecommendations': sum(user_behaviors.values()),
        'avgLatency': round(sum(item['value'] for item in system_metrics['end_to_end_latency'][-10:]) / 10, 2),
        'throughput': round(sum(item['value'] for item in system_metrics['kafka_throughput'][-10:]) / 10, 2)
    })

@app.route('/api/user-behavior/distribution', methods=['GET'])
def get_behavior_distribution():
    total = sum(user_behaviors.values())
    if total == 0:
        return jsonify([
            {'name': 'view', 'value': 0},
            {'name': 'click', 'value': 0},
            {'name': 'cart', 'value': 0},
            {'name': 'purchase', 'value': 0}
        ])
    
    return jsonify([
        {'name': 'view', 'value': user_behaviors['view']},
        {'name': 'click', 'value': user_behaviors['click']},
        {'name': 'cart', 'value': user_behaviors['cart']},
        {'name': 'purchase', 'value': user_behaviors['purchase']}
    ])

@app.route('/api/user-behavior/time-distribution', methods=['GET'])
def get_time_distribution():
    # 生成过去24小时的行为数据
    current_hour = datetime.datetime.now().hour
    data = []
    
    for i in range(24):
        hour = (current_hour - 23 + i) % 24
        data.append({
            'hour': hour,
            'view': random.randint(50, 200),
            'click': random.randint(30, 150),
            'cart': random.randint(10, 50),
            'purchase': random.randint(5, 30)
        })
    
    return jsonify(data)

@app.route('/api/user-behavior/hot-items', methods=['GET'])
def get_hot_items():
    return jsonify(hot_items)

@app.route('/api/user-behavior/activity', methods=['GET'])
def get_user_activity():
    result = []
    days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    
    for hour in range(24):
        for day in range(7):
            result.append({
                'hour': hour,
                'day': days[day],
                'value': user_activity[hour][day]
            })
    
    return jsonify(result)

@app.route('/api/recommendation/category-distribution', methods=['GET'])
def get_category_distribution():
    total = sum(recommendation_categories.values())
    if total == 0:
        return jsonify([
            {'name': category, 'value': 0} for category in recommendation_categories
        ])
    
    return jsonify([
        {'name': category, 'value': count} for category, count in recommendation_categories.items()
    ])

@app.route('/api/recommendation/reasons', methods=['GET'])
def get_recommendation_reasons():
    return jsonify(recommendation_reasons)

@app.route('/api/recommendation/conversion-rates', methods=['GET'])
def get_conversion_rates():
    return jsonify(conversion_rates)

@app.route('/api/recommendation/personal-history', methods=['GET'])
def get_personal_history():
    user_id = request.args.get('userId', 'user1')
    return jsonify([rec for rec in personal_recommendations if rec['userId'] == user_id])

@app.route('/api/system/kafka-metrics', methods=['GET'])
def get_kafka_metrics():
    return jsonify(system_metrics['kafka_throughput'])

@app.route('/api/system/flink-metrics', methods=['GET'])
def get_flink_metrics():
    return jsonify(system_metrics['flink_throughput'])

@app.route('/api/system/latency', methods=['GET'])
def get_latency():
    return jsonify(system_metrics['end_to_end_latency'])

@app.route('/api/system/resource-usage', methods=['GET'])
def get_resource_usage():
    return jsonify({
        'cpu': system_metrics['cpu_usage'],
        'memory': system_metrics['memory_usage']
    })

@app.route('/api/system/logs', methods=['GET'])
def get_logs():
    logs = []
    log_types = ['INFO', 'WARN', 'ERROR']
    log_sources = ['Kafka', 'Flink', 'Recommendation', 'User Behavior']
    log_messages = [
        'Processing completed successfully',
        'Connection established',
        'Data received from Kafka',
        'Recommendation generated',
        'User behavior processed',
        'Slow processing detected',
        'Connection timeout, retrying',
        'Memory usage high',
        'CPU usage spike detected',
        'Failed to process message'
    ]
    
    for i in range(20):
        log_type = log_types[0] if random.random() < 0.7 else (log_types[1] if random.random() < 0.8 else log_types[2])
        timestamp = time.time() - random.randint(0, 3600)
        
        logs.append({
            'id': i + 1,
            'timestamp': timestamp,
            'type': log_type,
            'source': random.choice(log_sources),
            'message': random.choice(log_messages)
        })
    
    logs.sort(key=lambda x: x['timestamp'], reverse=True)
    return jsonify(logs)

@socketio.on('connect')
def handle_connect():
    print('Client connected')

@socketio.on('disconnect')
def handle_disconnect():
    print('Client disconnected')

if __name__ == '__main__':
    # 启动实时数据生成线程
    data_thread = threading.Thread(target=generate_realtime_data)
    data_thread.daemon = True
    data_thread.start()
    
    # 启动Flask应用
    socketio.run(app, host='0.0.0.0', port=5000, debug=True, allow_unsafe_werkzeug=True)
