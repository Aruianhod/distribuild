import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Spin, Table, Tabs } from 'antd';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, BarChart, Bar, Legend } from 'recharts';
import axios from 'axios';
import { io } from 'socket.io-client';

const API_URL = 'http://localhost:5000/api';
const SOCKET_URL = 'http://localhost:5000';
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

const UserBehavior: React.FC = () => {
  const [behaviorDistribution, setBehaviorDistribution] = useState<any[]>([]);
  const [timeDistribution, setTimeDistribution] = useState<any[]>([]);
  const [hotItems, setHotItems] = useState<any[]>([]);
  const [userActivity, setUserActivity] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 获取用户行为分布数据
        const distributionResponse = await axios.get(`${API_URL}/user-behavior/distribution`);
        setBehaviorDistribution(distributionResponse.data);
        
        // 获取时间分布数据
        const timeResponse = await axios.get(`${API_URL}/user-behavior/time-distribution`);
        setTimeDistribution(timeResponse.data);
        
        // 获取热门商品数据
        const hotItemsResponse = await axios.get(`${API_URL}/user-behavior/hot-items`);
        setHotItems(hotItemsResponse.data);
        
        // 获取用户活跃度数据
        const activityResponse = await axios.get(`${API_URL}/user-behavior/activity`);
        setUserActivity(activityResponse.data);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching user behavior data:', error);
        setLoading(false);
      }
    };

    fetchData();

    // 设置定时刷新
    const intervalId = setInterval(fetchData, 30000); // 每30秒刷新一次

    // 连接WebSocket
    const socket = io(SOCKET_URL);
    
    socket.on('connect', () => {
      console.log('Connected to WebSocket server');
    });
    
    socket.on('user_behavior', (data) => {
      const parsedData = JSON.parse(data);
      // 更新用户行为分布
      setBehaviorDistribution([
        { name: 'view', value: parsedData.counts.view },
        { name: 'click', value: parsedData.counts.click },
        { name: 'cart', value: parsedData.counts.cart },
        { name: 'purchase', value: parsedData.counts.purchase }
      ]);
    });
    
    socket.on('disconnect', () => {
      console.log('Disconnected from WebSocket server');
    });

    // 清理函数
    return () => {
      clearInterval(intervalId);
      socket.disconnect();
    };
  }, []);

  // 热门商品表格列定义
  const columns = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      render: (_: any, __: any, index: number) => index + 1
    },
    {
      title: '商品ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '商品名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '热度',
      dataIndex: 'count',
      key: 'count',
      sorter: (a: any, b: any) => a.count - b.count,
      defaultSortOrder: 'descend' as 'descend',
    },
  ];

  // 用户活跃度热力图数据处理
  const heatmapData = userActivity.map((item) => ({
    ...item,
    value: item.value,
    hour: `${item.hour}:00`,
  }));

  // 计算每个小时的总活跃度
  const hourlyActivity: { [key: string]: number } = {};
  timeDistribution.forEach((item) => {
    const total = item.view + item.click + item.cart + item.purchase;
    hourlyActivity[item.hour] = total;
  });

  // 转换为图表数据
  const hourlyActivityData = Object.keys(hourlyActivity).map((hour) => ({
    hour: `${hour}:00`,
    活跃度: hourlyActivity[hour],
  }));

  return (
    <div className="user-behavior">
      <h2>用户行为分析</h2>
      
      <Spin spinning={loading}>
        <Tabs defaultActiveKey="1">
          <Tabs.TabPane tab="行为分布" key="1">
            <Row gutter={16}>
              <Col span={12}>
                <Card title="用户行为类型分布">
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={behaviorDistribution}
                        cx="50%"
                        cy="50%"
                        labelLine={true}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        nameKey="name"
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      >
                        {behaviorDistribution.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
              <Col span={12}>
                <Card title="用户行为时间分布">
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart
                      data={timeDistribution}
                      margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="hour" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Area type="monotone" dataKey="view" stackId="1" stroke="#8884d8" fill="#8884d8" />
                      <Area type="monotone" dataKey="click" stackId="1" stroke="#82ca9d" fill="#82ca9d" />
                      <Area type="monotone" dataKey="cart" stackId="1" stroke="#ffc658" fill="#ffc658" />
                      <Area type="monotone" dataKey="purchase" stackId="1" stroke="#ff8042" fill="#ff8042" />
                    </AreaChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="热门商品" key="2">
            <Card title="热门商品排行榜">
              <Table 
                dataSource={hotItems} 
                columns={columns} 
                rowKey="id"
                pagination={false}
              />
            </Card>
            <Card title="热门商品分布" style={{ marginTop: '20px' }}>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={hotItems.slice(0, 10)}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="count" name="热度" fill="#8884d8" />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="用户活跃度" key="3">
            <Card title="每小时用户活跃度">
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart
                  data={hourlyActivityData}
                  margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="hour" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area type="monotone" dataKey="活跃度" stroke="#8884d8" fill="#8884d8" />
                </AreaChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
        </Tabs>
      </Spin>
    </div>
  );
};

export default UserBehavior;
