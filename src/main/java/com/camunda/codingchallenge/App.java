package com.camunda.codingchallenge;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import org.camunda.bpm.engine.impl.util.json.JSONObject;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

public class App {

	private static HttpURLConnection connection;

	public static void main(String[] args) {

		BufferedReader reader;
		String line;
		StringBuffer responseContent = new StringBuffer();

		try {
			URL url = new URL(
					"https://elxkoom6p4.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			int status = connection.getResponseCode();

			if (status > 299) {
				reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					responseContent.append(line);
				}
				reader.close();
			} else {
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				while ((line = reader.readLine()) != null) {
					responseContent.append(line);
				}
				reader.close();
			}
			
			JSONObject myObject = new JSONObject(responseContent.toString());
			String bpmn = myObject.getString("bpmn20Xml");
			InputStream stream = new ByteArrayInputStream(bpmn.getBytes(StandardCharsets.UTF_8));
			BpmnModelInstance modelInstance = Bpmn.readModelFromStream(stream);

			FlowNode startNode = modelInstance.getModelElementById(args[0]);
			FlowNode endNode = modelInstance.getModelElementById(args[1]);
			FlowNode exclusiveNode = modelInstance.getModelElementById("approveInvoice");
			FlowNode node = endNode;

			ArrayList<FlowNode> nodes = new ArrayList<FlowNode>();
			ArrayList<String> list = new ArrayList<String>();

			nodes.add(node);

			while (!(node.getIncoming().isEmpty()) && node != startNode) {
				for (SequenceFlow sequenceFlow : node.getIncoming()) {
					nodes.add(sequenceFlow.getSource());
					node = sequenceFlow.getSource();
				}
			}

			if (nodes.contains(exclusiveNode)) {
				for (SequenceFlow sf : exclusiveNode.getIncoming()) {
					FlowNode fn = sf.getSource();
					for (SequenceFlow sff : fn.getIncoming()) {

						if (!nodes.contains(sff.getSource())) {
							nodes.remove(fn);
						}
					}
				}
			}
			Collections.reverse(nodes);
			for (FlowNode flowNode : nodes) {
				list.add(flowNode.getId());
			}
			System.out.println("The path from " + startNode.getId() + " to " + endNode.getId() + " is: \n" + list);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			connection.disconnect();
		}
	}

}
