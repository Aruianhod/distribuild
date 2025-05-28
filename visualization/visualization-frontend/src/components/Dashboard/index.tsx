import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Spin } from 'antd';
import { UserOutlined, ThunderboltOutlined, ClockCircleOutlined, RiseOutlined } from '@ant-design/icons';
import axios from 'axios';
import { io } from 'socket.io-client';
import RealtimeBehaviorChart from './RealtimeBehaviorChart';
import RealtimeRecommendationChart from './RealtimeRecommendationChart';

const API_URL = 'http://localhost:5000/api';
const SOCKET_URL = 'http://localhost:5000';

const Dashboard: React.FC = () => {
  const [kpiData, setKpiData] = useState({
    activeUsers: 0,
    totalRecommendations: 0,
    avgLatency: 0,
    throughput: 0
  });
  const [loading, setLoading] = useState(true);
  const [behaviorData, setBehaviorData] = useState<any[]>([]);
  const [recommendationData, setRecommendationData] = useState<any[]>([]);
  const [systemMetrics, setSystemMetrics] = useState<any>({});

  useEffect(() => {
    // 获取KPI数据
    const fetchKpiData = async () => {
      try {
        const response = await axios.get(`${API_URL}/dashboard/kpi`);
        setKpiData(response.data);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching KPI data:', error);
        setLoading(false);
      }
    };

    fetchKpiData();

    // 设置定时刷新
    const intervalId = setInterval(fetchKpiData, 30000); // 每30秒刷新一次

    // 连接WebSocket
    const socket = io(SOCKET_URL);
    
    socket.on('connect', () => {
      console.log('Connected to WebSocket server');
    });
    
    socket.on('user_behavior', (data) => {
      const parsedData = JSON.parse(data);
      setBehaviorData(prev => [...prev, parsedData].slice(-20)); // 保留最近20条数据
    });
    
    socket.on('recommendation', (data) => {
      const parsedData = JSON.parse(data);
      setRecommendationData(prev => [...prev, parsedData].slice(-20)); // 保留最近20条数据
    });
    
    socket.on('system_metrics', (data) => {
      const parsedData = JSON.parse(data);
      setSystemMetrics(parsedData);
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

  return (
    <div className="dashboard">
      <h2>系统仪表盘</h2>
      
      <Spin spinning={loading}>
        <Row gutter={16}>
          <Col span={6}>
            <Card>
              <Statistic
                title="当前活跃用户"
                value={kpiData.activeUsers}
                prefix={<UserOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="今日推荐总数"
                value={kpiData.totalRecommendations}
                prefix={<RiseOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="平均推荐延迟"
                value={kpiData.avgLatency}
                suffix="ms"
                prefix={<ClockCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="系统吞吐量"
                value={kpiData.throughput}
                suffix="msg/s"
                prefix={<ThunderboltOutlined />}
              />
            </Card>
          </Col>
        </Row>
      </Spin>
      
      <Row gutter={16} style={{ marginTop: '20px' }}>
        <Col span={12}>
          <Card title="实时用户行为">
            <RealtimeBehaviorChart data={behaviorData} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="实时推荐结果">
            <RealtimeRecommendationChart data={recommendationData} />
          </Card>
        </Col>
      </Row>
      
      <Row gutter={16} style={{ marginTop: '20px' }}>
        <Col span={24}>
          <Card title="系统状态">
            <Row gutter={16}>
              <Col span={8}>
                <Statistic
                  title="Kafka吞吐量"
                  value={systemMetrics.kafka_throughput?.value || 0}
                  suffix="msg/s"
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="Flink吞吐量"
                  value={systemMetrics.flink_throughput?.value || 0}
                  suffix="msg/s"
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="端到端延迟"
                  value={systemMetrics.end_to_end_latency?.value || 0}
                  suffix="ms"
                />
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
