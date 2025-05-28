import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

interface RealtimeBehaviorChartProps {
  data: any[];
}

const RealtimeBehaviorChart: React.FC<RealtimeBehaviorChartProps> = ({ data }) => {
  // 处理数据，转换为图表所需格式
  const processedData = data.map(item => {
    const timestamp = new Date(item.timestamp * 1000).toLocaleTimeString();
    return {
      time: timestamp,
      type: item.type
    };
  });

  // 统计各类型行为的累计数量
  const behaviorCounts = {
    view: 0,
    click: 0,
    cart: 0,
    purchase: 0
  };

  const chartData = processedData.map((item, index) => {
    if (item.type in behaviorCounts) {
      behaviorCounts[item.type as keyof typeof behaviorCounts]++;
    }
    
    return {
      time: item.time,
      view: behaviorCounts.view,
      click: behaviorCounts.click,
      cart: behaviorCounts.cart,
      purchase: behaviorCounts.purchase
    };
  });

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart
        data={chartData}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="time" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Line type="monotone" dataKey="view" name="浏览" stroke="#8884d8" />
        <Line type="monotone" dataKey="click" name="点击" stroke="#82ca9d" />
        <Line type="monotone" dataKey="cart" name="加购" stroke="#ffc658" />
        <Line type="monotone" dataKey="purchase" name="购买" stroke="#ff8042" />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default RealtimeBehaviorChart;
