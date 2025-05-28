import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Spin, Tabs, Table, Badge } from 'antd';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area, BarChart, Bar } from 'recharts';
import axios from 'axios';
import { io } from 'socket.io-client';

const API_URL = 'http://localhost:5000/api';
const SOCKET_URL = 'http://localhost:5000';

const SystemPerformance: React.FC = () => {
  const [kafkaMetrics, setKafkaMetrics] = useState<any[]>([]);
  const [flinkMetrics, setFlinkMetrics] = useState<any[]>([]);
  const [latencyData, setLatencyData] = useState<any[]>([]);
  const [resourceUsage, setResourceUsage] = useState<any>({
    cpu: [],
    memory: []
  });
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 获取Kafka指标数据
        const kafkaResponse = await axios.get(`${API_URL}/system/kafka-metrics`);
        setKafkaMetrics(kafkaResponse.data);
        
        // 获取Flink指标数据
        const flinkResponse = await axios.get(`${API_URL}/system/flink-metrics`);
        setFlinkMetrics(flinkResponse.data);
        
        // 获取延迟数据
        const latencyResponse = await axios.get(`${API_URL}/system/latency`);
        setLatencyData(latencyResponse.data);
        
        // 获取资源使用情况
        const resourceResponse = await axios.get(`${API_URL}/system/resource-usage`);
        setResourceUsage(resourceResponse.data);
        
        // 获取系统日志
        const logsResponse = await axios.get(`${API_URL}/system/logs`);
        setLogs(logsResponse.data);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching system performance data:', error);
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
    
    socket.on('system_metrics', (data) => {
      const parsedData = JSON.parse(data);
      
      // 更新Kafka指标
      if (parsedData.kafka_throughput) {
        setKafkaMetrics(prev => {
          const newData = [...prev, parsedData.kafka_throughput];
          return newData.slice(-30); // 保留最近30条数据
        });
      }
      
      // 更新Flink指标
      if (parsedData.flink_throughput) {
        setFlinkMetrics(prev => {
          const newData = [...prev, parsedData.flink_throughput];
          return newData.slice(-30); // 保留最近30条数据
        });
      }
      
      // 更新延迟数据
      if (parsedData.end_to_end_latency) {
        setLatencyData(prev => {
          const newData = [...prev, parsedData.end_to_end_latency];
          return newData.slice(-30); // 保留最近30条数据
        });
      }
      
      // 更新资源使用情况
      if (parsedData.cpu_usage && parsedData.memory_usage) {
        setResourceUsage(prev => ({
          cpu: [...prev.cpu, parsedData.cpu_usage].slice(-30),
          memory: [...prev.memory, parsedData.memory_usage].slice(-30)
        }));
      }
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
    const date = new Date(timestamp * 1000);
    return date.toLocaleTimeString();
  };

  // 处理图表数据
  const processChartData = (data: any[]) => {
    return data.map(item => ({
      ...item,
      time: formatTimestamp(item.timestamp)
    }));
  };

  // 处理资源使用数据
  const processResourceData = () => {
    const result = [];
    const cpuData = resourceUsage.cpu || [];
    const memoryData = resourceUsage.memory || [];
    
    for (let i = 0; i < Math.min(cpuData.length, memoryData.length); i++) {
      result.push({
        time: formatTimestamp(cpuData[i].timestamp),
        cpu: cpuData[i].value,
        memory: memoryData[i].value
      });
    }
    
    return result;
  };

  // 日志表格列定义
  const columns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (timestamp: number) => new Date(timestamp * 1000).toLocaleString()
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => {
        let color = 'green';
        if (type === 'WARN') {
          color = 'orange';
        } else if (type === 'ERROR') {
          color = 'red';
        }
        return <Badge color={color} text={type} />;
      }
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
    }
  ];

  return (
    <div className="system-performance">
      <h2>系统性能监控</h2>
      
      <Spin spinning={loading}>
        <Tabs defaultActiveKey="1">
          <Tabs.TabPane tab="Kafka性能" key="1">
            <Card title="Kafka消息吞吐量">
              <ResponsiveContainer width="100%" height={300}>
                <LineChart
                  data={processChartData(kafkaMetrics)}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="value" name="消息数/秒" stroke="#8884d8" />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="Flink性能" key="2">
            <Card title="Flink作业吞吐量">
              <ResponsiveContainer width="100%" height={300}>
                <LineChart
                  data={processChartData(flinkMetrics)}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="value" name="处理数/秒" stroke="#82ca9d" />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="延迟分析" key="3">
            <Card title="端到端延迟">
              <ResponsiveContainer width="100%" height={300}>
                <LineChart
                  data={processChartData(latencyData)}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="value" name="延迟(ms)" stroke="#ff7300" />
                </LineChart>
              </ResponsiveContainer>
            </Card>
            <Card title="延迟分布" style={{ marginTop: '20px' }}>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={[
                    { range: '0-100ms', count: latencyData.filter(item => item.value < 100).length },
                    { range: '100-200ms', count: latencyData.filter(item => item.value >= 100 && item.value < 200).length },
                    { range: '200-300ms', count: latencyData.filter(item => item.value >= 200 && item.value < 300).length },
                    { range: '300-400ms', count: latencyData.filter(item => item.value >= 300 && item.value < 400).length },
                    { range: '400-500ms', count: latencyData.filter(item => item.value >= 400 && item.value < 500).length },
                    { range: '500ms+', count: latencyData.filter(item => item.value >= 500).length }
                  ]}
                  margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="range" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="count" name="数量" fill="#8884d8" />
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="资源使用" key="4">
            <Card title="CPU和内存使用率">
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart
                  data={processResourceData()}
                  margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area type="monotone" dataKey="cpu" name="CPU使用率(%)" stroke="#8884d8" fill="#8884d8" />
                  <Area type="monotone" dataKey="memory" name="内存使用率(%)" stroke="#82ca9d" fill="#82ca9d" />
                </AreaChart>
              </ResponsiveContainer>
            </Card>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="系统日志" key="5">
            <Card title="系统日志">
              <Table 
                dataSource={logs} 
                columns={columns} 
                rowKey="id"
                pagination={{ pageSize: 10 }}
              />
            </Card>
          </Tabs.TabPane>
        </Tabs>
      </Spin>
    </div>
  );
};

export default SystemPerformance;
