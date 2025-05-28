import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { io } from 'socket.io-client';
import { Card, Row, Col, Button, Alert, Spin, message } from 'antd';

const API_URL = 'http://localhost:5000/api';
const SOCKET_URL = 'http://localhost:5000';

const VisualizationTest: React.FC = () => {
  const [apiStatus, setApiStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [socketStatus, setSocketStatus] = useState<'disconnected' | 'connected' | 'error'>('disconnected');
  const [dataFlow, setDataFlow] = useState<{[key: string]: boolean}>({
    userBehavior: false,
    recommendation: false,
    systemMetrics: false
  });
  const [messageCount, setMessageCount] = useState(0);
  const [testResults, setTestResults] = useState<string[]>([]);

  useEffect(() => {
    // 测试API连接
    const testApi = async () => {
      try {
        await axios.get(`${API_URL}/dashboard/kpi`);
        setApiStatus('success');
        addTestResult('✅ REST API连接成功');
      } catch (error) {
        console.error('API连接失败:', error);
        setApiStatus('error');
        addTestResult('❌ REST API连接失败');
      }
    };

    // 测试WebSocket连接
    const testSocket = () => {
      const socket = io(SOCKET_URL);
      
      socket.on('connect', () => {
        setSocketStatus('connected');
        addTestResult('✅ WebSocket连接成功');
        
        // 监听数据流
        socket.on('user_behavior', (data) => {
          setDataFlow(prev => ({ ...prev, userBehavior: true }));
          setMessageCount(prev => prev + 1);
          if (!dataFlow.userBehavior) {
            addTestResult('✅ 用户行为数据流接收成功');
          }
        });
        
        socket.on('recommendation', (data) => {
          setDataFlow(prev => ({ ...prev, recommendation: true }));
          setMessageCount(prev => prev + 1);
          if (!dataFlow.recommendation) {
            addTestResult('✅ 推荐结果数据流接收成功');
          }
        });
        
        socket.on('system_metrics', (data) => {
          setDataFlow(prev => ({ ...prev, systemMetrics: true }));
          setMessageCount(prev => prev + 1);
          if (!dataFlow.systemMetrics) {
            addTestResult('✅ 系统指标数据流接收成功');
          }
        });
      });
      
      socket.on('connect_error', (error) => {
        console.error('WebSocket连接错误:', error);
        setSocketStatus('error');
        addTestResult('❌ WebSocket连接失败');
      });
      
      socket.on('disconnect', () => {
        setSocketStatus('disconnected');
        addTestResult('⚠️ WebSocket断开连接');
      });
      
      return socket;
    };

    testApi();
    const socket = testSocket();

    // 清理函数
    return () => {
      socket.disconnect();
    };
  }, []);

  const addTestResult = (result: string) => {
    setTestResults(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${result}`]);
  };

  const runApiTests = async () => {
    message.info('开始测试API接口...');
    
    try {
      // 测试仪表盘API
      await axios.get(`${API_URL}/dashboard/kpi`);
      addTestResult('✅ 仪表盘KPI数据接口正常');
      
      // 测试用户行为API
      await axios.get(`${API_URL}/user-behavior/distribution`);
      addTestResult('✅ 用户行为分布数据接口正常');
      
      await axios.get(`${API_URL}/user-behavior/time-distribution`);
      addTestResult('✅ 用户行为时间分布数据接口正常');
      
      await axios.get(`${API_URL}/user-behavior/hot-items`);
      addTestResult('✅ 热门商品数据接口正常');
      
      // 测试推荐结果API
      await axios.get(`${API_URL}/recommendation/category-distribution`);
      addTestResult('✅ 推荐类别分布数据接口正常');
      
      await axios.get(`${API_URL}/recommendation/reasons`);
      addTestResult('✅ 推荐理由数据接口正常');
      
      // 测试系统性能API
      await axios.get(`${API_URL}/system/kafka-metrics`);
      addTestResult('✅ Kafka指标数据接口正常');
      
      await axios.get(`${API_URL}/system/latency`);
      addTestResult('✅ 延迟数据接口正常');
      
      message.success('API接口测试完成，所有接口正常');
    } catch (error) {
      console.error('API测试失败:', error);
      addTestResult('❌ API测试失败，请检查控制台错误');
      message.error('API测试失败，请检查控制台错误');
    }
  };

  return (
    <div className="visualization-test">
      <h2>可视化功能验证</h2>
      
      <Row gutter={16}>
        <Col span={12}>
          <Card title="连接状态">
            <p>
              REST API状态: {
                apiStatus === 'loading' ? <Spin size="small" /> :
                apiStatus === 'success' ? <Alert message="连接成功" type="success" showIcon /> :
                <Alert message="连接失败" type="error" showIcon />
              }
            </p>
            <p>
              WebSocket状态: {
                socketStatus === 'connected' ? <Alert message="已连接" type="success" showIcon /> :
                socketStatus === 'disconnected' ? <Alert message="未连接" type="warning" showIcon /> :
                <Alert message="连接错误" type="error" showIcon />
              }
            </p>
            <p>接收到的消息数: {messageCount}</p>
            <p>
              数据流状态:
              <ul>
                <li>用户行为数据: {dataFlow.userBehavior ? '✅ 已接收' : '❌ 未接收'}</li>
                <li>推荐结果数据: {dataFlow.recommendation ? '✅ 已接收' : '❌ 未接收'}</li>
                <li>系统指标数据: {dataFlow.systemMetrics ? '✅ 已接收' : '❌ 未接收'}</li>
              </ul>
            </p>
            <Button type="primary" onClick={runApiTests}>测试API接口</Button>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="测试结果" style={{ maxHeight: '400px', overflow: 'auto' }}>
            {testResults.length === 0 ? (
              <p>等待测试结果...</p>
            ) : (
              <ul>
                {testResults.map((result, index) => (
                  <li key={index}>{result}</li>
                ))}
              </ul>
            )}
          </Card>
        </Col>
      </Row>
      
      <Row style={{ marginTop: '20px' }}>
        <Col span={24}>
          <Alert
            message="验证说明"
            description={
              <div>
                <p>本页面用于验证可视化功能的完整性与效果。请检查以下内容：</p>
                <ol>
                  <li>REST API连接是否正常</li>
                  <li>WebSocket连接是否正常</li>
                  <li>是否能接收到实时数据流</li>
                  <li>各API接口是否正常工作</li>
                </ol>
                <p>如果所有检查项都通过，说明可视化功能正常工作。</p>
              </div>
            }
            type="info"
            showIcon
          />
        </Col>
      </Row>
    </div>
  );
};

export default VisualizationTest;
