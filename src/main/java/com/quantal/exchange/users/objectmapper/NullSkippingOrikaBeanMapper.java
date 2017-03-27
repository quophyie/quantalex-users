package com.quantal.exchange.users.objectmapper;

/**
 * Created by dman on 08/03/2017.
 */

import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Orika mapper exposed as a Spring Bean. It contains the configuration for the mapper factory and factory builder. It will scan
 * the Spring application context searching for mappers and converters to register them into the factory. To use it we just need
 * to autowire it into our class.
 *
 * @author dlizarra
 *
 */
@Component
public class NullSkippingOrikaBeanMapper extends OrikaBeanMapper {


  private boolean mapNulls = true;
  public NullSkippingOrikaBeanMapper(@Value("${app.orikamapper.map-nulls}") final Boolean mapNulls) {
    this.mapNulls = mapNulls;
  }

  /**
   * Configures the mapper factory builder
   * {@inheritDoc}
   */
  @Override
  protected void configureFactoryBuilder(final DefaultMapperFactory.Builder factoryBuilder) {
    // If this.mapNulls is false, then null values will not be mapped i.e they will be skipped
    factoryBuilder.mapNulls(this.mapNulls);
  }

}