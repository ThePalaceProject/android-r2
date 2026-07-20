import { requireNotNull } from './notnull';

/**
 * The type of value receivers. Each evaluation will receive the
 * old value of the attribute, and the new value that has just been
 * set.
 */

export type ReceiverType<T> = (valueOld: T, valueNew: T) => void;

/**
 * A subscription to an observable attribute.
 */

export interface SubscriptionType {
  unsubscribe(): void;
}

/**
 * A simple "hot observable" attribute. Registered subscribers receive
 * the current value of the attribute on subscription, and receive value
 * updates each time the value is written.
 */

export class Attribute<T> {
  private value: T;
  private subscribers: Map<number, ReceiverType<T>>;
  private subscriberNext = 0;

  private constructor(initial: T) {
    this.value = requireNotNull(initial, 'initial');
    this.subscribers = new Map();
  }

  /**
   * Create a new attribute from the given value.
   */

  public static create<T>(initial: T) {
    return new Attribute(initial);
  }

  /**
   * Get the current attribute value.
   */

  public valueNow(): T {
    return this.value;
  }

  /**
   * Set the value of the attribute. This will call all subscribers with the
   * new value.
   */

  public set(valueNew: T) {
    const valueOld = this.value;
    this.value = valueNew;

    this.subscribers.forEach((e) => {
      try {
        e(valueOld, valueNew);
      } catch (error) {
        console.error('Subscriber failed to handle value change:', error);
      }
    });
  }

  /**
   * Subscribe to the attribute. The receiver will be immediately notified
   * with the current value and will receive all value updates until it
   * unsubscribes.
   */

  public subscribe(r: ReceiverType<T>): SubscriptionType {
    const id = this.subscriberNext;
    ++this.subscriberNext;
    this.subscribers.set(id, r);

    try {
      r(this.value, this.value);
    } catch (error) {
      console.error('Subscriber failed to handle value change:', error);
    }

    return {
      unsubscribe: () => {
        this.subscribers.delete(id);
      },
    };
  }
}
