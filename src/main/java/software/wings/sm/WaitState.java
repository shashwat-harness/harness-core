/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 */
public class WaitState extends State {
  private static final long serialVersionUID = 1L;

  /**
   * @param name
   * @param stateType
   */
  public WaitState(String name) {
    super(name, StateType.WAIT.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // TODO Auto-generated method stub
    return null;
  }
}
