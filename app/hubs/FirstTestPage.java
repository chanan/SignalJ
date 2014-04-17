package hubs;
import java.util.UUID;

import models.Person;

public interface FirstTestPage {
	public void firstTestFunction();
	public void firstTestFunctionWithParam(String param);
	public void twoParams(int int1, int int2);
	public void complexObj(Person person);
	public void calledFromClient(UUID connectionId);
	public void notCalledFromClient(UUID connectionId);
	public void sendToGroup(String message);
	public void thisWillBeCalledFromOutsideTheHub(String message);
}