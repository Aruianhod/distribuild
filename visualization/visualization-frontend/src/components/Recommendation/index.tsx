import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Spin, Tabs, Timeline } from 'antd';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, LineChart, Line, XAxis, YAxis, CartesianGrid } from 'recharts';
import { CloudOutlined } from '@ant-design/icons';
import axios from 'axios';
import { io } from 'socket.io-client';
import ReactWordcloud from 'react-wordcloud';

const API_URL = 'http://localhost:5000/api';
const SOCKET_URL = 'http://localhost:5000';
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D'];

const Recommendation: React.FC = () => {
  const [categoryDistribution, setCategoryDistribution] = useState<any[]>([]);
  const [recommendationReasons, setRecommendationReasons] = useState<any[]>([]);
  const [conversionRates, setConversionRates] = useState<any[]>([]);
  const [personalHistory, setPersonalHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 获取推荐类别分布数据
        const categoryResponse = await axios.get(`${API_URL}/recommendation/category-distribution`);
        setCategoryDistribution(categoryResponse.data);
        
        // 获取推荐理由数据
        const reasonsResponse = await axios.get(`${API_URL}/recommendation/reasons`);
        setRecommendationReasons(reasonsResponse.data);
        
        // 获取转化率数据
        const conversionResponse = await axios.get(`${API_URL}/recommendation/conversion-rates`);
        setConversionRates(conversionResponse.data);
        
        // 获取个人推荐历史
        const historyResponse = await axios.get(`${API_URL}/recommendation/personal-history?userId=user1`);
        setPersonalHistory(historyResponse.data);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching recommendation data:', error);
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
    
    socket.on('recommendation', (data) => {
      const parsedData = JSON.parse(data);
      // 更新推荐类别分布
      const newDistribution = Object.keys(parsedData.counts).map(category => ({
        name: category,
        value: parsedData.counts[category]
      }));
      setCategoryDistribution(newDistribution);
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

  // 格式化时间戳
  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  // 词云配置
  const wordcloudOptions = {
    rotations: 2,
    rotationAngles: [0, 90],
    fontSizes: [15, 50],
  };

  return (
    <div className="recommendation">
      <h2>推荐效果分析</h2>
      
      <Spin spinning={loading}>
        <Tabs defaultActiveKey="1">
          <Tabs.TabPane tab="推荐分布" key="1">
            <Row gutter={16}>
              <Col span={12}>
                <Card title="推荐商品类别分布">
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={categoryDistribution}
                        cx="50%"
                        cy="50%"
                        labelLine={true}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        nameKey="name"
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      >
                        {categoryDistribution.map((entry, index) => (
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
                <Card title="推荐理由词云">
                  <div style={{ height: 300 }}>
                    {recommendationReasons.length > 0 && (
                      <ReactWordcloud
                        words={recommendationReasons}
                        options={wordcloudOptions}
                      />
                    )}
                  </div>
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="推荐效果" key="2">
            <Card title="推荐转化率趋势">
              <ResponsiveContainer width="100%" height={300}>
                <LineChart
                  data={conversionRates}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="clickRate" name="点击率" stroke="#8884d8" />
                  <Line type="monotone" dataKey="cartRate" name="加购率" stroke="#82ca9d" />
                  <Line type="monotone" dataKey="purchaseRate" name="购买率" stroke="#ff7300" />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="个人推荐" key="3">
            <Card title="用户个人推荐历史记录">
              <Timeline mode="left">
                {personalHistory.map((rec, index) => (
                  <Timeline.Item key={index} dot={<CloudOutlined style={{ fontSize: '16px' }} />}>
                    <p><strong>推荐时间：</strong>{formatTimestamp(rec.timestamp)}</p>
                    <p><strong>推荐商品：</strong></p>
                    <ul>
                      {rec.items.map((item: any, itemIndex: number) => (
                        <li key={itemIndex}>
                          {item.name} (得分: {item.score}) - {item.reason}
                        </li>
                      ))}
                    </ul>
                  </Timeline.Item>
                ))}
              </Timeline>
            </Card>
          </Tabs.TabPane>
        </Tabs>
      </Spin>
    </div>
  );
};

export default Recommendation;
